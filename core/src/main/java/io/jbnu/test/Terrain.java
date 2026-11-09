package io.jbnu.test;

/** Authoritative building-local collision and drawing geometry. Support IDs index platforms. */
public final class Terrain {
    private Terrain() {}
    public static final float WIDTH=600, FLOOR_HEIGHT=160;
    private static final float[][][] BUILDINGS={make(10),make(20),make(30)};
    public static int floorCount(int stage){if(stage<0||stage>2)throw new IllegalArgumentException("stage");return (stage+1)*10;}
    public static float top(int depth){return -depth*FLOOR_HEIGHT;}
    /** Shared read-only rows: left, right, top, local depth. */
    public static float[][] platforms(int stage){floorCount(stage);return BUILDINGS[stage];}
    private static float[][] make(int floors){
        float[][] rows=new float[2+3*(floors-1)][4];int id=0;
        rows[id++]=new float[]{90,510,0,0};
        for(int d=1;d<floors;d++)for(int lane=0;lane<3;lane++){
            float left=(d%2==1?50:100)+185*lane;
            rows[id++]=new float[]{left,left+100,top(d),d};
        }
        rows[id]=new float[]{30,570,top(floors),floors};return rows;
    }
    public static boolean contains(int stage,int id,float x){float[] p=platforms(stage)[id];return x>=p[0]&&x<=p[1];}
    public static boolean fragile(int stage,int id){float d=platforms(stage)[id][3];return stage==2&&d>0&&d<floorCount(stage);}
    public static float[] fire(int stage,int id){
        float[] p=platforms(stage)[id];int d=(int)p[3];
        if(stage==0||d==0||d==floorCount(stage)||(id-1)%3!=1)return null;
        return new float[]{p[0]+35,p[0]+65,p[2],p[2]+36};
    }
    public static int[] windowSize(int displayWidth,int displayHeight){
        if(displayWidth<160||displayHeight<160)throw new IllegalArgumentException("display bounds");
        float scale=Math.min(1,Math.min((displayWidth-80)/1280f,(displayHeight-100)/720f));
        return new int[]{(int)(1280*scale),(int)(720*scale)};
    }
}
