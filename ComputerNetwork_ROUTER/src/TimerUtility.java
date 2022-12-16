
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class TimerUtility {

    public static int TIME_UNIT = 1000;

    public static class Event {
        String ID;
        int elapseTime;
        int expirTime;
        Function func;

        public Event(String ID, int elapseTime, int expirTime, Function func){
            this.ID = ID;
            this.elapseTime = elapseTime;
            this.expirTime = expirTime;
            this.func = func;
        }
    }

    public interface Function {
        void doSomething();
    }

    private static Queue<Event> eventQue = new LinkedList<>();

    static {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                int loopInx = 0;
                int loopCnt = eventQue.size();

                while(loopInx++ < loopCnt){
                    Event ev = eventQue.poll();

                    if(ev.elapseTime >= ev.expirTime){
                        if (ev.func == null) continue;
                        ev.func.doSomething();
                        continue;
                    }
                    else {
                        ev.elapseTime += TIME_UNIT;
                        eventQue.add(ev);
                    }
                }
            }
        }, 0, TIME_UNIT);
    }

    private static void deleteEvent(Event event){
        event.func = null;
        event.ID = "";
        event.elapseTime = event.expirTime;
    }

    public static void SetTimeout(String eventID, int timer, Function func){
        if(timer == 0) func.doSomething();
        eventQue.add(new Event(eventID, 0, timer, func));
    }

    public static void Cancel(String eventID){
        for (Event ev : eventQue){
            if(ev.ID == eventID){
                deleteEvent(ev);
            }
        }
    }

    public static void Alter(String eventID, int timer){
        for (Event ev : eventQue){
            if(ev.ID == eventID){
                ev.elapseTime = timer;
            }
        }
    }

    public static void Reset(String eventID){
        Alter(eventID, 0);
    }

}