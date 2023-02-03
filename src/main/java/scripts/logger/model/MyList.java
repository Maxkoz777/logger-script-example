package scripts.logger.model;

import java.util.Collection;
import java.util.LinkedList;

public class MyList<T> extends LinkedList<T> {

    public MyList(Collection<? extends T> c) {
        super(c);
    }

    public int getModCount() {
        return modCount;
    }


}
