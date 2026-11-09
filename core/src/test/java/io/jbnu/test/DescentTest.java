package io.jbnu.test;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Random;
import java.lang.reflect.Field;

public class DescentTest {
 static class Counting extends Random {int calls;double value;Counting(double value){this.value=value;}@Override public double nextDouble(){calls++;return value;}}
 static void set(GameRules g,String name,Object value)throws Exception{Field f=GameRules.class.getDeclaredField(name);f.setAccessible(true);f.set(g,value);}
 static GameRules ready(Counting r){GameRules g=new GameRules(4,r);g.startOrNext();return g;}
 static void until(GameRules g,GameRules.Phase phase){for(int i=0;i<4000&&g.phase()!=phase;i++)g.update(.01f);assertEquals(phase,g.phase());}
 static void policy(GameRules g){float target=g.anchor()==0?515:g.anchor()%2==1?525:465;g.move(Math.abs(g.x()-target)<3?0:g.x()<target?1:-1);}
 static void section(GameRules g){for(int i=0;i<30000&&g.phase()!=GameRules.Phase.SHOP&&g.phase()!=GameRules.Phase.ALL_CLEAR;i++){if(g.phase()==GameRules.Phase.READY)policy(g);else g.move(0);g.update(.01f);}assertTrue(g.phase()==GameRules.Phase.SHOP||g.phase()==GameRules.Phase.ALL_CLEAR);}
 static void airborne(GameRules g,int anchor,float x,float y)throws Exception{set(g,"anchor",anchor);set(g,"x",x);set(g,"y",y);set(g,"vy",0f);set(g,"grounded",false);set(g,"support",-1);g.move(0);}
 @Test public void probabilityAndDisplay(){int[] n={1,2,3,5,8,10};double[] p={.9,.729,.531441,.205891132094649,.0225283995449392,.00304325272217045};for(int i=0;i<n.length;i++)assertEquals(p[i],GameRules.survival(n[i]),1e-14);double old=1;for(int d=1;d<=30;d++){double next=GameRules.survival(d);assertTrue(next>0&&next<old);old=next;assertNotEquals("0.00%",GameRules.percent(next));assertNotEquals("100.00%",GameRules.deathPercent(next));}assertEquals(1,GameRules.survival(-2),0);}
 @Test public void ordinaryWholeCampaignAndReset(){Counting r=new Counting(0);GameRules g=ready(r);for(int s=0;s<3;s++){section(g);assertEquals(Terrain.floorCount(s),g.anchor());assertEquals((s+1)*(s+2)*500,g.coins());assertEquals(0,g.deaths());if(s<2)assertTrue(g.leaveShop());}assertEquals(GameRules.Phase.ALL_CLEAR,g.phase());assertEquals(60,r.calls);assertEquals(60,g.streak());float time=g.runSeconds();g.update(5);assertEquals(time,g.runSeconds(),0);g.restart();assertEquals(0,g.coins());assertEquals(0,g.stageBank());assertEquals(0,g.anchor());assertEquals(0,g.stage());assertEquals(0,g.runSeconds(),0);assertTrue(Double.isNaN(g.sample()));}
 @Test public void actualOneAndMultipleFloorContactSamplesOnce()throws Exception{for(int n=1;n<=3;n++){Counting r=new Counting(0);GameRules g=ready(r);airborne(g,0,n%2==0?520:470,Terrain.top(n-1)-1);until(g,GameRules.Phase.SPINNING);assertEquals(n,g.drop());assertEquals(1,r.calls);double sample=g.sample();g.update(.2f);assertEquals(sample,g.sample(),0);assertEquals(0,g.coins());until(g,GameRules.Phase.READY);assertEquals(100*n,g.coins());assertEquals(n,g.anchor());g.update(.1f);assertEquals(1,r.calls);assertEquals(100*n,g.coins());}}
 @Test public void sameAndUpwardBounceCannotFarm()throws Exception{Counting r=new Counting(0);GameRules g=ready(r);assertTrue(g.jump());g.update(1);assertEquals(0,r.calls);airborne(g,3,300,20);g.update(1);assertEquals(0,r.calls);airborne(g,3,520,-170);g.update(1);assertEquals(0,r.calls);assertEquals(0,g.coins());}
 @Test public void savedBurnsDepthWithoutReward()throws Exception{Counting r=new Counting(.99);GameRules g=ready(r);set(g,"cushion",true);airborne(g,0,470,-1);until(g,GameRules.Phase.SPINNING);until(g,GameRules.Phase.READY);assertEquals("SAVED",g.appliedResult());assertEquals(1,g.streak());assertEquals(1,g.anchor());assertEquals(0,g.coins());assertEquals(0,g.resolvedAward());assertFalse(g.cushion());g.jump();g.update(1);assertEquals(1,r.calls);assertEquals(0,g.coins());}
 @Test public void shopsBankAndConsumables()throws Exception{Counting r=new Counting(0);GameRules g=ready(r);assertFalse(g.buyCoffee());section(g);g.togglePause();assertFalse(g.buyCoffee());assertFalse(g.leaveShop());g.togglePause();assertTrue(g.buyCushion());assertFalse(g.buyCushion());assertTrue(g.buyCoffee());assertFalse(g.buyCoffee());assertEquals(200,g.coins());assertTrue(g.leaveShop());assertEquals(200,g.stageBank());assertTrue(g.cushion());assertTrue(g.coffee());assertTrue(g.jump());assertTrue(g.jump());assertFalse(g.coffee());assertFalse(g.jump());
  GameRules poor=ready(new Counting(0));section(poor);assertTrue(poor.buyCushion());assertTrue(poor.buyCoffee());assertFalse(poor.buyCoffee());assertEquals(200,poor.coins());
  GameRules bank=ready(r);section(bank);assertTrue(bank.buyCoffee());bank.leaveShop();assertEquals(800,bank.stageBank());for(int i=0;i<2;i++){set(bank,"coins",900);r.value=.99;airborne(bank,0,470,-1);until(bank,GameRules.Phase.DEAD);assertEquals(800,bank.coins());assertEquals(0,bank.streak());until(bank,GameRules.Phase.READY);assertEquals(0,bank.anchor());assertFalse(bank.coffee());}r.value=0;section(bank);assertEquals(2800,bank.coins());}
 @Test public void allFrozenClocksAndCollapse()throws Exception{GameRules g=ready(new Counting(0));set(g,"phase",GameRules.Phase.SHOP);g.leaveShop();set(g,"phase",GameRules.Phase.SHOP);g.leaveShop();airborne(g,0,470,-1);until(g,GameRules.Phase.SPINNING);float w=g.worldSeconds(),c=g.collapse(3);g.update(.2f);assertEquals(w,g.worldSeconds(),0);assertEquals(c,g.collapse(3),0);g.togglePause();float t=g.runSeconds(),p=g.phaseTime();g.update(10);assertEquals(t,g.runSeconds(),0);assertEquals(p,g.phaseTime(),0);assertEquals(c,g.collapse(3),0);g.togglePause();until(g,GameRules.Phase.READY);g.update(.2f);assertTrue(g.collapse(3)>c);g.togglePause();w=g.worldSeconds();c=g.collapse(3);g.update(9);assertEquals(w,g.worldSeconds(),0);assertEquals(c,g.collapse(3),0);g.togglePause();g.update(2.1f);assertTrue(g.broken(3));assertFalse(g.grounded());
  GameRules shop=ready(new Counting(0));section(shop);w=shop.worldSeconds();t=shop.runSeconds();float x=shop.x();shop.move(1);shop.update(10);assertEquals(w,shop.worldSeconds(),0);assertEquals(t,shop.runSeconds(),0);assertEquals(x,shop.x(),0);
 }
 @Test public void topologyAndFireBeatsCushion()throws Exception{
  for(int stage=0;stage<3;stage++){float[][] rows=Terrain.platforms(stage);for(int id=0;id<rows.length;id++){
   int d=(int)rows[id][3];assertEquals(-160*d,rows[id][2],0);
   assertEquals(stage==2&&d>0&&d<Terrain.floorCount(stage),Terrain.fragile(stage,id));
   if(stage==0||d==0||d==Terrain.floorCount(stage))assertNull(Terrain.fire(stage,id));
  }}
  Counting r=new Counting(0);GameRules g=ready(r);set(g,"phase",GameRules.Phase.SHOP);g.leaveShop();set(g,"cushion",true);
  float[] fire=Terrain.fire(1,2);assertNotNull(fire);airborne(g,0,(fire[0]+fire[1])/2,fire[2]+1);g.update(.02f);
  assertEquals(GameRules.Phase.DEAD,g.phase());assertEquals("FIRE",g.deathCause());assertEquals(0,r.calls);
 }
 @Test public void insufficientFundsAndRestartInPause()throws Exception{GameRules g=ready(new Counting(0));set(g,"phase",GameRules.Phase.SHOP);assertFalse(g.buyCoffee());assertFalse(g.buyCushion());assertEquals(0,g.coins());g.togglePause();g.restart();assertFalse(g.paused());assertEquals(GameRules.Phase.READY,g.phase());assertEquals("NONE",g.appliedResult());}
 @Test public void ordinarySeededRetriesReachExitWithoutReseeding(){
  GameRules g=new GameRules(4);g.startOrNext();int shops=0;
  for(int i=0;i<150000&&g.phase()!=GameRules.Phase.ALL_CLEAR;i++){
   if(g.phase()==GameRules.Phase.SHOP){g.buyCushion();g.buyCoffee();g.leaveShop();shops++;}
   if(g.phase()==GameRules.Phase.READY)policy(g);else g.move(0);g.update(.01f);
  }
  assertEquals(GameRules.Phase.ALL_CLEAR,g.phase());assertEquals(2,shops);assertTrue(g.deaths()<100);assertEquals(30,g.anchor());
 }
}
