package io.jbnu.test;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.*;

/** Pixel descent; visible ledges and colliders share Terrain coordinates. */
public final class Main extends ApplicationAdapter {
    private SpriteBatch batch; private ShapeRenderer shapes; private BitmapFont font; private TextureAtlas elder;
    private final Map<String,Texture> art=new LinkedHashMap<String,Texture>();
    private OrthographicCamera camera,hud; private FitViewport worldViewport,hudViewport;
    private GameRules game; private long sequence;
    private static final String[] ART={"building-wall","window-dark","window-fire","ledge-solid","ledge-cracked","ledge-broken","roof","ground","exit","fire-strip","smoke-a","smoke-b","sky","coin"};
    @Override public void create(){
        batch=new SpriteBatch();shapes=new ShapeRenderer();font=new BitmapFont(Gdx.files.internal("pixel-v10/font.fnt"));
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        elder=new TextureAtlas(Gdx.files.internal("pixel-v10/atlas.atlas"));
        for(Texture t:elder.getTextures())t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        // Missing final assets fail visibly rather than silently substituting generated art.
        for(String name:ART){Texture t=new Texture(Gdx.files.internal("descent/"+name+".png"));t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);art.put(name,t);}
        camera=new OrthographicCamera();hud=new OrthographicCamera();worldViewport=new FitViewport(800,720,camera);hudViewport=new FitViewport(1280,720,hud);
        game=new GameRules(Long.getLong("roulette.seed",new Random().nextLong()));resize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
    }
    @Override public void render(){
        input();game.update(Math.min(Gdx.graphics.getDeltaTime(),1f/30));
        camera.position.set(300,Math.max(Terrain.top(Terrain.floorCount(game.stage()))+180,Math.min(-180,game.y()-180)),0);camera.update();
        ScreenUtils.clear(.025f,.03f,.045f,1);drawWorld();drawHud();trace();
    }
    private boolean down(int a,int b){return Gdx.input.isKeyPressed(a)||Gdx.input.isKeyPressed(b);}
    private void input(){
        Vector3 p=new Vector3(Gdx.input.getX(),Gdx.input.getY(),0);hudViewport.unproject(p);
        if(game.phase()==GameRules.Phase.INTRO){
            if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)||(Gdx.input.justTouched()&&in(p,460,240,360,56)))game.startOrNext();
            return;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.P)||Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))game.togglePause();
        if(Gdx.input.isKeyJustPressed(Input.Keys.R))game.restart();
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){if(game.phase()==GameRules.Phase.SHOP)game.leaveShop();else game.startOrNext();}
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1))game.buyCushion();
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2))game.buyCoffee();
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)||Gdx.input.isKeyJustPressed(Input.Keys.UP))game.jump();
        int move=(down(Input.Keys.D,Input.Keys.RIGHT)?1:0)-(down(Input.Keys.A,Input.Keys.LEFT)?1:0);
        if(Gdx.input.isTouched()&&!game.paused()&&game.phase()==GameRules.Phase.READY){if(in(p,20,20,90,48))move=-1;if(in(p,120,20,90,48))move=1;}
        game.move(move);
        if(!Gdx.input.justTouched())return;
        if(in(p,1130,654,130,46)){game.togglePause();return;}
        if(game.paused()){if(in(p,460,240,360,56))game.togglePause();return;}
        if(game.phase()==GameRules.Phase.ALL_CLEAR&&in(p,460,240,360,56))game.restart();
        if(game.phase()==GameRules.Phase.SHOP){if(in(p,300,350,300,56))game.buyCushion();if(in(p,680,350,300,56))game.buyCoffee();if(in(p,460,240,360,56))game.leaveShop();}
        if(in(p,220,20,140,48))game.jump();
    }
    private boolean in(Vector3 p,float x,float y,float w,float h){return p.x>=x&&p.x<x+w&&p.y>=y&&p.y<y+h;}
    private void tile(String name,float x,float y,float w,float h){batch.draw(art.get(name),x,y,w,h);}
    private void drawWorld(){
        worldViewport.apply();batch.setProjectionMatrix(camera.combined);batch.begin();batch.setColor(Color.WHITE);
        float bottom=camera.position.y-360,top=camera.position.y+360;
        tile("sky",-100,bottom,800,720);
        for(float y=(float)Math.floor(bottom/64)*64;y<Math.min(top,0);y+=64)for(int x=30;x<570;x+=60)tile("building-wall",x,y,60,64);
        float[][] platforms=Terrain.platforms(game.stage());int floors=Terrain.floorCount(game.stage());
        for(int d=1;d<=floors;d++)if(Terrain.top(d)>bottom-80&&Terrain.top(d)<top+100)
            for(int x=100;x<560;x+=140)tile(game.stage()>0&&d<floors?"window-fire":"window-dark",x,Terrain.top(d)+55,48,56);
        for(int id=0;id<platforms.length;id++){
            float[] p=platforms[id];int d=(int)p[3];
            if(p[2]<bottom-80||p[2]>top+100||game.broken(id))continue;
            String surface=d==0?"roof":d==floors?"ground":game.collapse(id)>=0?"ledge-cracked":"ledge-solid";
            for(float x=p[0];x<p[1];x+=32)tile(surface,x,p[2]-20,Math.min(32,p[1]-x),20);
            float[] f=Terrain.fire(game.stage(),id);
            if(f!=null){int frame=(int)(game.worldSeconds()*8)%4;batch.draw(new TextureRegion(art.get("fire-strip"),frame*32,0,32,32),f[0],f[2],f[1]-f[0],f[3]-f[2]);}
            if(d==floors)tile("exit",280,p[2],64,64);
        }
        // Foreground smoke obscures lower surfaces, not the actor or the nearest landing clue.
        for(int d=1;d<floors;d++)if(game.stage()==2&&Terrain.top(d)<game.y()-200&&Terrain.top(d)>bottom-145){
            float drift=(float)Math.sin(game.worldSeconds()*.7+d)*28;
            batch.setColor(1,1,1,.95f);tile(d%2==0?"smoke-a":"smoke-b",20+drift,Terrain.top(d)-45,560,160);
        }
        batch.setColor(Color.WHITE);
        String pose=game.phase()==GameRules.Phase.DEAD?"dead":!game.grounded()?"air":Math.abs(game.vx())>0&&((int)(game.worldSeconds()*8)%2==0)?"walk":"idle";
        batch.draw(elder.findRegion("elder-"+pose),game.x()-28,game.y(),56,64);
        font.getData().setScale(2);font.setColor(Color.WHITE);
        for(int d=0;d<=floors;d++)if(Terrain.top(d)>bottom&&Terrain.top(d)<top)
            font.draw(batch,d==0?"ROOF":d==floors?"GROUND / EXIT":"DEPTH "+d,40,Terrain.top(d)+24);
        for(int id=0;id<platforms.length;id++)if(game.collapse(id)>=0&&!game.broken(id)&&platforms[id][2]>bottom&&platforms[id][2]<top)
            font.draw(batch,String.format(Locale.ROOT,"%.1f",Math.max(0,GameRules.COLLAPSE_SECONDS-game.collapse(id))),platforms[id][0]+35,platforms[id][2]+15);
        batch.end();
    }
    private void rect(float x,float y,float w,float h,Color c){shapes.setColor(c);shapes.rect(x,y,w,h);}
    private void text(String s,float x,float y,int scale){font.setColor(Color.WHITE);font.getData().setScale(scale);font.draw(batch,s.replace("~","EST "),x,y);}
    private void button(float x,float y,float w,String label){rect(x,y,w,56,Color.valueOf("30485a"));}
    private String shopStatus(boolean cushionItem){
        if(cushionItem?game.cushion():game.coffee())return "OWNED";
        int price=cushionItem?600:200;
        return game.coins()<price?"NEED "+(price-game.coins())+" COINS":"BUY "+price;
    }
    // A small literal lowercase pixel alphabet avoids the atlas's uppercase-copy lowercase glyphs.
    private void title(){
        String letters="abceiloprsty";
        String[] pixels={"00000/00000/01110/00001/01111/10001/01111","10000/10000/10110/11001/10001/10001/11110","00000/00000/01111/10000/10000/10000/01111","00000/00000/01110/10001/11111/10000/01111","00100/00000/01100/00100/00100/00100/01110","01100/00100/00100/00100/00100/00100/01110","00000/00000/01110/10001/10001/10001/01110","00000/11110/10001/10001/11110/10000/10000","00000/00000/10110/11001/10000/10000/10000","00000/00000/01111/10000/01110/00001/11110","00100/00100/11111/00100/00100/00100/00011","00000/10001/10001/10001/01111/00001/01110"};
        int x=210;
        for(char c:"escape probability".toCharArray()){
            if(c!=' '){String[] rows=pixels[letters.indexOf(c)].split("/");
                for(int y=0;y<7;y++)for(int col=0;col<5;col++)if(rows[y].charAt(col)=='1')rect(x+col*8,550-y*8,8,8,Color.valueOf("f4e5ba"));}
            x+=48;
        }
    }
    private void drawIntro(){
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        rect(0,0,1280,720,Color.valueOf("101b2b"));rect(190,475,900,4,Color.valueOf("e8b76a"));title();
        button(460,240,360,"");shapes.end();batch.begin();
        for(int x=170;x<410;x+=60)for(int y=70;y<420;y+=64)tile("building-wall",x,y,60,64);
        for(int x=200;x<400;x+=100)for(int y=100;y<410;y+=100)tile("window-fire",x,y,48,56);
        tile("roof",150,420,280,20);tile("ground",850,190,220,20);
        batch.draw(elder.findRegion("elder-idle"),920,210,84,96);
        text("START [ENTER]",480,275,3);text("A/D MOVE   SPACE JUMP",460,205,2);
        batch.end();
    }
    private void drawWheel(){
        rect(0,0,1280,630,new Color(.01f,.02f,.04f,.8f));
        double rotation=game.wheelRotation();
        Color cream=Color.valueOf("f4e5ba"),ink=Color.valueOf("101b2b"),live=Color.valueOf("6cbf91"),dead=Color.valueOf("c95754");
        for(int y=-195;y<=195;y+=5)for(int x=-195;x<=195;x+=5){
            int r=x*x+y*y;if(r>197*197)continue;
            Color color;
            if(r>187*187)color=cream;
            else if(r<64*64)color=ink;
            else {
                // atan2(x,y) has its zero at the fixed top pointer, clockwise positive.
                double angle=Math.toDegrees(Math.atan2(x,y));
                double fraction=GameRules.wheelFractionAtPointer(rotation-angle);
                color=fraction<game.probability()?live:dead;
            }
            rect(640+x,330+y,5,5,color);
        }
        for(int row=0;row<8;row++)rect(620+row*2.5f,545-row*5,40-row*5,5,Color.valueOf("f4e5ba"));
    }
    private void drawHud(){
        hudViewport.apply();batch.setProjectionMatrix(hud.combined);shapes.setProjectionMatrix(hud.combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
        if(game.phase()==GameRules.Phase.INTRO){drawIntro();return;}
        boolean focus=game.phase()==GameRules.Phase.SPINNING||game.phase()==GameRules.Phase.REVEAL;
        boolean overlay=game.paused()||game.phase()==GameRules.Phase.SHOP||game.phase()==GameRules.Phase.ALL_CLEAR;
        shapes.begin(ShapeRenderer.ShapeType.Filled);rect(0,630,1280,90,new Color(.025f,.04f,.06f,.94f));
        if(game.popup()>0)rect(390,671,300,4,Color.valueOf(game.appliedResult().equals("SAVED")?"e8b76a":"6cbf91"));
        button(1130,654,130,"");button(20,20,90,"");button(120,20,90,"");button(220,20,140,"");
        if(focus&&!overlay)drawWheel();
        if(overlay){rect(160,200,960,390,new Color(.025f,.04f,.06f,.97f));button(460,240,360,"");if(game.phase()==GameRules.Phase.SHOP&&!game.paused()){
            rect(300,350,300,56,Color.valueOf(!game.cushion()&&game.coins()>=600?"37674f":"283746"));
            rect(680,350,300,56,Color.valueOf(!game.coffee()&&game.coins()>=200?"37674f":"283746"));}}
        shapes.end();batch.begin();
        tile("coin",20,668,24,24);text("COINS "+game.coins(),52,691,2);text("STREAK "+game.streak(),390,704,game.popup()>1.05f?4:3);
        if(game.popup()>0)text(game.appliedResult()+" +"+game.resolvedAward(),730,694,2);
        text("BUILDING "+(game.stage()+1)+"/3  "+game.anchor()+"/"+Terrain.floorCount(game.stage()),22,654,2);
        text(String.format(Locale.ROOT,"TIME %.1f",game.runSeconds()),390,654,2);
        if(game.cushion())text("CUSHION",620,654,2);
        if(game.coffee())text("COFFEE",740,654,2);
        text(game.paused()?"RESUME":"PAUSE [P]",1140,686,2);text("A / <",32,53,2);text("D / >",132,53,2);text("JUMP",242,53,2);
        if(!game.grounded()&&!focus&&!overlay)text("DROP EST "+game.expectedDrop()+"  DEAD "+GameRules.deathPercent(GameRules.survival(game.expectedDrop())),760,615,2);
        if(focus&&!overlay){
            text("DROP "+game.drop()+" / ROULETTE",460,605,2);
            text("LIVE "+GameRules.percent(game.probability())+" / DEAD "+GameRules.deathPercent(game.probability()),410,105,2);
            text(game.phase()==GameRules.Phase.REVEAL?game.revealResult():"SPIN",590,340,2);
        }
        if(overlay){
            text(game.paused()?"PAUSED":game.phase()==GameRules.Phase.SHOP?"GROUND / SHOP":"GROUND EXIT / ALL CLEAR",260,548,3);
            text(game.paused()?"P / ESC RESUME     R NEW RUN":"A/D MOVE   SPACE JUMP   R NEW RUN",260,492,2);
            if(game.phase()==GameRules.Phase.SHOP&&!game.paused()){
                text("[1] CUSHION",312,397,2);text(shopStatus(true),312,371,2);
                text("[2] COFFEE",692,397,2);text(shopStatus(false),692,371,2);
                text("CUSHION: ONE ROULETTE SAVE   COFFEE: AIR JUMP",260,440,2);
            }
            text(game.paused()?"RESUME":game.phase()==GameRules.Phase.SHOP?"CONTINUE / SKIP [ENTER]":"NEW RUN [R]",480,275,2);
        }
        if(game.phase()==GameRules.Phase.DEAD&&!overlay)text("DEAD / "+game.deathCause()+" / BUILDING RETRY",360,580,2);
        batch.end();
    }
    private void trace(){
        String path=System.getProperty("roulette.qa.trace");if(path==null)return;
        StringBuilder s=new StringBuilder();
        add(s,"version",14);add(s,"floorCount",Terrain.floorCount(game.stage()));add(s,"supportDepth",game.support()<0?-1:(int)Terrain.platforms(game.stage())[game.support()][3]);
        add(s,"wheelRotation",game.wheelRotation());add(s,"pointerFraction",GameRules.wheelFractionAtPointer(game.wheelRotation()));add(s,"revealResult",game.revealResult());add(s,"seq",++sequence);add(s,"phase",game.phase());add(s,"paused",game.paused());add(s,"stage",game.stage());add(s,"depth",game.depth());add(s,"anchor",game.anchor());add(s,"support",game.support());
        add(s,"sample",game.sample());add(s,"probability",game.probability());add(s,"samples",game.samples());add(s,"drop",game.drop());add(s,"rawResult",game.rawResult());add(s,"appliedResult",game.appliedResult());add(s,"award",game.resolvedAward());
        add(s,"coins",game.coins());add(s,"stageBank",game.stageBank());add(s,"streak",game.streak());add(s,"deaths",game.deaths());add(s,"cause",game.deathCause());add(s,"seed",game.seed());
        add(s,"x",game.x());add(s,"y",game.y());add(s,"vx",game.vx());add(s,"vy",game.vy());add(s,"grounded",game.grounded());add(s,"worldSeconds",game.worldSeconds());add(s,"hazardTime",game.hazardTime());add(s,"runSeconds",game.runSeconds());add(s,"phaseTime",game.phaseTime());add(s,"collapse",game.collapseTrace());add(s,"cushion",game.cushion());add(s,"coffee",game.coffee());
        add(s,"width",Gdx.graphics.getWidth());add(s,"height",Gdx.graphics.getHeight());add(s,"viewportX",hudViewport.getScreenX());add(s,"viewportY",hudViewport.getScreenY());add(s,"viewportWidth",hudViewport.getScreenWidth());add(s,"viewportHeight",hudViewport.getScreenHeight());add(s,"cameraY",camera.position.y);
        Path target=Paths.get(path),tmp=target.resolveSibling(target.getFileName()+".tmp");
        try{Files.write(tmp,s.toString().getBytes(StandardCharsets.UTF_8));Files.move(tmp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){throw new IllegalStateException("QA trace",e);}
    }
    private void add(StringBuilder s,String key,Object value){s.append(key).append('=').append(value).append('\n');}
    @Override public void resize(int w,int h){worldViewport.update(w,h);hudViewport.update(w,h,true);}
    /** A focus/minimize event is idempotent; returning to the window requires explicit resume. */
    @Override public void pause(){if(game!=null&&!game.paused())game.togglePause();}
    @Override public void dispose(){batch.dispose();shapes.dispose();font.dispose();elder.dispose();for(Texture t:art.values())t.dispose();}
}
