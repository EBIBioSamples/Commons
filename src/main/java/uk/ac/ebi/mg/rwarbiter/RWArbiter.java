package uk.ac.ebi.mg.rwarbiter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RWArbiter<TT extends TokenW>
{
 private ReentrantLock lock = new ReentrantLock();
 
 private Condition writeReleased = lock.newCondition();
 private Condition readReleased = lock.newCondition();
 
 private int readReqs=0;
 private TT writeToken=null;
 
 private TokenFactory<TT> factory;
 
 public RWArbiter( TokenFactory<TT> tf )
 {
  factory = tf;
 }
 
 public TT getReadLock()
 {
  try
  {
   lock.lock();
   
   while( writeToken != null )
    writeReleased.awaitUninterruptibly();

   readReqs++;
   
   return factory.createToken();
  }
  finally
  {
   lock.unlock(); 
  }
 }
 
 public boolean isWriteToken( TokenW t )
 {
  return writeToken == t;
 }
 
 public TT getWriteLock()
 {
  try
  {
   lock.lock();
   
   while( writeToken != null )
    writeReleased.awaitUninterruptibly();

   writeToken=factory.createToken();
   
   if( readReqs > 0 )
    readReleased.awaitUninterruptibly();
   
   return writeToken;
  }
  finally
  {
   lock.unlock(); 
  }
 }

// public boolean checkTokenValid( Object tobj )
// {
//  return  tobj instanceof ReadWriteToken && ((ReadWriteToken)tobj).isActive();
// }
 
 public void releaseLock( TokenW tobj ) throws InvalidTokenException
 {
  if( ! tobj.isActive() )
   throw new InvalidTokenException();
  
  try
  {
   lock.lock();
   
   if( writeToken == tobj )
   {
    writeToken.setActive(false);
    writeToken = null;
    
    writeReleased.signalAll();
   }
   else
   {
    tobj.setActive(false);
    
    readReqs--;
    
    if( readReqs == 0 )
     readReleased.signal();
   }
  }
  finally
  {
   lock.unlock(); 
  }
  
 }
 
 public static class ReadWriteToken implements TokenW
 {
  boolean active = true;

  public boolean isActive()
  {
   return active;
  }

  public void setActive(boolean active)
  {
   this.active = active;
  }
 }
}
