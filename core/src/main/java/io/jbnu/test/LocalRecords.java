package io.jbnu.test;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Four completion buckets, independent minima. IO belongs to the application, never GameRules. */
public final class LocalRecords {
    private static final int MAX_RECORD_BYTES=64*1024;
    private static final String[] BUCKETS={"2","3","4","MIXED"};
    private final Path directory,file;
    private final Map<String,double[]> best=new LinkedHashMap<String,double[]>();
    private byte[] corrupt;
    private boolean readable=true;
    private String status="EMPTY";
    public LocalRecords(Path directory){
        this.directory=directory.toAbsolutePath().normalize();file=this.directory.resolve("records-v1.properties");
        try {
            if(!Files.exists(file))return;
            byte[] bytes=new byte[MAX_RECORD_BYTES+1];int length=0;
            // Bound the stream itself: a size precheck would race with file growth.
            try(InputStream in=Files.newInputStream(file)){
                while(length<bytes.length){
                    int count=in.read(bytes,length,bytes.length-length);
                    if(count<0)break;
                    length+=count;
                }
            }
            if(length>MAX_RECORD_BYTES)throw new IOException("record exceeds 64 KiB");
            bytes=Arrays.copyOf(bytes,length);
            try {
                Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));
                if(!"1".equals(p.getProperty("version")))throw new IOException("version");
                for(String bucket:BUCKETS)if(p.containsKey(bucket)) {
                    String[] values=p.getProperty(bucket).split(",",-1);
                    if(values.length!=3)throw new IOException("metrics");
                    double[] row={Double.parseDouble(values[0]),Double.parseDouble(values[1]),Double.parseDouble(values[2])};
                    validate(row);best.put(bucket,row);
                }
                if(p.size()!=best.size()+1)throw new IOException("unknown keys");status="LOADED";
            }catch(Exception invalid){best.clear();corrupt=bytes;status="CORRUPT_PRESERVED_PENDING";}
        }catch(Exception inaccessible){readable=false;status="UNREADABLE";}
    }
    public static Path defaultDirectory(){return Paths.get(System.getProperty("roulette.dataDir",Paths.get(System.getProperty("user.home"),".roulette-runner").toString()));}
    private static void validate(double[] row)throws IOException {
        if(!Double.isFinite(row[0])||row[0]<=0||row[0]>315576000)throw new IOException("time range");
        for(int i=1;i<3;i++)if(!Double.isFinite(row[i])||row[i]<0||row[i]>Integer.MAX_VALUE||row[i]!=Math.rint(row[i]))throw new IOException("counter range");
    }
    /** Descent is deliberately not comparable with historical fixed-sector buckets. */
    public boolean complete(GameRules game){return false;}
    public double[] best(String bucket){double[] row=best.get(bucket);return row==null?null:row.clone();}
    public String summary(String bucket){double[] row=best.get(bucket);return row==null?bucket+": no completion":String.format(Locale.ROOT,"%s: %.2fs / %d deaths / %d jumps",bucket,row[0],(long)row[1],(long)row[2]);}
    public String status(){return status;} public String dataUri(){return directory.toUri().toASCIIString();}
}
