package io.jbnu.test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/** Deterministic input-driven descent. Rendering and records never consume this RNG. */
public final class GameRules {
    public enum Phase { INTRO, READY, SPINNING, REVEAL, DEAD, SHOP, ALL_CLEAR }
    public static final float SPEED=220, GRAVITY=1000, JUMP_SPEED=360, HEIGHT=60, HALF_WIDTH=12;
    public static final float SPIN_SECONDS=.45f, REVEAL_SECONDS=.35f, RESPAWN_SECONDS=.6f, COLLAPSE_SECONDS=2.2f;
    private final long seed; private final Random random;
    private float[] collapse;
    private Phase phase=Phase.INTRO;
    private int stage,anchor,depth,support,moveIntent,coins,stageBank,streak,samples,deaths,jumps,drop,award,runSerial;
    private float x,y,vx,vy,worldSeconds,runSeconds,phaseTime,popup;
    private double sample=Double.NaN,probability=1;
    private boolean grounded=true,paused,cushion,coffee;
    private String result="NONE",cause="NONE";
    public GameRules(long seed){this(seed,new Random(seed));}
    GameRules(long seed,Random random){this.seed=seed;this.random=random;spawn();phase=Phase.INTRO;}
    public static double survival(int n){return n<=0?1:Math.pow(.9,n*(n+1.0)/2.0);}
    public static String percent(double p){
        if(p>0&&p<.0001)return String.format(Locale.ROOT,"%.2e%%",p*100);
        if(p<1&&p>.9999)return ">99.99%";
        return String.format(Locale.ROOT,"%.2f%%",p*100);
    }
    public static String deathPercent(double q){return q>0&&q<.0001?">99.99%":percent(1-q);}
    public static double wheelRotation(double sample){return -360*sample;}
    public static double wheelFractionAtPointer(double rotation){
        double fraction=-rotation/360;
        return fraction>=0&&fraction<1?fraction:fraction-Math.floor(fraction);
    }
    public double wheelRotation(){
        double target=wheelRotation(sample);
        if(phase!=Phase.SPINNING)return target;
        double remaining=1-Math.min(1,phaseTime/SPIN_SECONDS);
        return target+1080*remaining*remaining*remaining;
    }
    public String revealResult(){return rawResult().equals("DEAD")&&cushion?"SAVED":rawResult();}
    public boolean startOrNext(){if(paused||phase!=Phase.INTRO)return false;phase=Phase.READY;return true;}
    public void move(int direction){if(direction< -1||direction>1)throw new IllegalArgumentException("direction");if(!paused)moveIntent=direction;}
    public boolean jump(){
        if(paused||phase!=Phase.READY||(!grounded&&!coffee))return false;
        if(!grounded)coffee=false;grounded=false;support=-1;vy=JUMP_SPEED;jumps++;return true;
    }
    public boolean togglePause(){if(phase==Phase.INTRO)return false;paused=!paused;return true;}
    public void restart(){stage=anchor=depth=coins=stageBank=streak=samples=deaths=jumps=0;worldSeconds=runSeconds=0;paused=cushion=coffee=false;runSerial++;spawn();}
    private void clearEvent(){sample=Double.NaN;probability=1;drop=award=0;popup=0;result="NONE";}
    private void spawn(){
        collapse=new float[Terrain.platforms(stage).length];Arrays.fill(collapse,-1);anchor=depth=0;support=0;x=300;y=0;vx=vy=0;
        grounded=true;moveIntent=0;clearEvent();phase=Phase.READY;phaseTime=0;
    }
    public boolean buyCushion(){if(!canBuy(600)||cushion)return false;coins-=600;cushion=true;return true;}
    public boolean buyCoffee(){if(!canBuy(200)||coffee)return false;coins-=200;coffee=true;return true;}
    private boolean canBuy(int price){return !paused&&phase==Phase.SHOP&&coins>=price;}
    public boolean leaveShop(){if(paused||phase!=Phase.SHOP)return false;stageBank=coins;stage++;spawn();return true;}
    public void update(float dt){
        if(!Float.isFinite(dt)||dt<0)throw new IllegalArgumentException("finite nonnegative dt");
        if(paused)return;
        while(dt>0){float h=Math.min(dt,1f/240);tick(h);dt-=h;}
    }
    private void tick(float dt){
        if(phase==Phase.INTRO||phase==Phase.SHOP||phase==Phase.ALL_CLEAR)return;
        runSeconds+=dt;
        if(phase!=Phase.READY){
            phaseTime+=dt;
            if(phase==Phase.SPINNING&&phaseTime>=SPIN_SECONDS)transition(Phase.REVEAL);
            else if(phase==Phase.REVEAL&&phaseTime>=REVEAL_SECONDS)resolve();
            else if(phase==Phase.DEAD&&phaseTime>=RESPAWN_SECONDS)spawn();
            return;
        }
        worldSeconds+=dt;popup=Math.max(0,popup-dt);
        for(int id=0;id<collapse.length;id++)if(collapse[id]>=0)collapse[id]+=dt;
        float[][] platforms=Terrain.platforms(stage);
        vx=moveIntent*SPEED;x=Math.max(HALF_WIDTH,Math.min(Terrain.WIDTH-HALF_WIDTH,x+vx*dt));
        if(grounded&&(!Terrain.contains(stage,support,x)||broken(support))){grounded=false;support=-1;}
        float oldY=y;int landed=-1;
        if(!grounded){
            vy-=GRAVITY*dt;y+=vy*dt;
            if(vy<=0)for(int id=0;id<platforms.length;id++){
                float top=platforms[id][2];
                if(!broken(id)&&Terrain.contains(stage,id,x)&&oldY>=top&&y<=top){y=top;vy=0;grounded=true;support=id;landed=id;break;}
            }
        }
        // Fire wins simultaneous landing, and cannot be saved by a cushion.
        for(int id=0;id<platforms.length;id++){
            float[] f=broken(id)?null:Terrain.fire(stage,id);
            if(f!=null&&x+HALF_WIDTH>f[0]&&x-HALF_WIDTH<f[1]&&y<f[3]&&y+HEIGHT>f[2]){die("FIRE");return;}
        }
        if(y<Terrain.top(Terrain.floorCount(stage))-240){die("FALL");return;}
        if(landed>=0){
            depth=(int)platforms[landed][3];if(Terrain.fragile(stage,landed)&&collapse[landed]<0)collapse[landed]=0;
            if(depth>anchor){clearEvent();drop=depth-anchor;probability=survival(drop);sample=random.nextDouble();samples++;result="PENDING";transition(Phase.SPINNING);}
        }
    }
    private void resolve(){
        if(sample>=probability&&!cushion){result="DEAD";die("ROULETTE");return;}
        if(sample>=probability){cushion=false;result="SAVED";award=0;}else{result="LIVE";award=100*drop;coins+=award;}
        streak++;anchor=depth;popup=1.3f;transition(Phase.READY);
        if(depth==Terrain.floorCount(stage)){popup=0;award=0;transition(stage==2?Phase.ALL_CLEAR:Phase.SHOP);}
    }
    private void die(String why){cause=why;coins=stageBank;streak=0;cushion=coffee=false;award=0;popup=0;deaths++;vx=vy=0;transition(Phase.DEAD);}
    private void transition(Phase p){phase=p;phaseTime=0;}
    public boolean broken(int d){return d>=0&&d<collapse.length&&collapse[d]>=COLLAPSE_SECONDS;}
    public float collapse(int d){return collapse[d];}
    public String collapseTrace(){return Arrays.toString(collapse).replace(" ","");}
    public int expectedDrop(){return Math.max(0,Math.min(Terrain.floorCount(stage),(int)Math.floor(-y/Terrain.FLOOR_HEIGHT)+1)-anchor);}
    public Phase phase(){return phase;} public boolean paused(){return paused;} public int stage(){return stage;}
    public int depth(){return depth;} public int anchor(){return anchor;} public int support(){return support;}
    public int coins(){return coins;} public int stageBank(){return stageBank;} public int streak(){return streak;}
    public int samples(){return samples;} public int deaths(){return deaths;} public int jumps(){return jumps;}
    public int drop(){return drop;} public int resolvedAward(){return award;} public float popup(){return popup;}
    public double sample(){return sample;} public double probability(){return probability;}
    public String rawResult(){return Double.isNaN(sample)?"NONE":sample<probability?"LIVE":"DEAD";}
    public String appliedResult(){return result;} public String deathCause(){return cause;}
    public boolean cushion(){return cushion;} public boolean coffee(){return coffee;} public boolean grounded(){return grounded;}
    public float x(){return x;} public float y(){return y;} public float vx(){return vx;} public float vy(){return vy;}
    public float worldSeconds(){return worldSeconds;} public float hazardTime(){return worldSeconds;}
    public float runSeconds(){return runSeconds;} public float phaseTime(){return phaseTime;} public long seed(){return seed;}
    public int runSerial(){return runSerial;}
}
