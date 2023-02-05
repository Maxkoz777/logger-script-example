package scripts.logger.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class MyList<T> extends LinkedList<T> {

    public MyList(Collection<? extends T> c) {
        super(c);
    }

    public int getModCount() {
        return modCount;
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        ListIterator<String> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            String s = listIterator.next();
            if (listIterator.hasNext() && s.equals("b")) {
                String c = listIterator.next();
                s += c;
                listIterator.remove();
                if (listIterator.hasPrevious()) {
                    listIterator.previous();
                    listIterator.set(s);
                }
            }
        }
        System.out.println(list);
    }


}
