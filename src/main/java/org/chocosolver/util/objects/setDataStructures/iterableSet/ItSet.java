package org.chocosolver.util.objects.setDataStructures.iterableSet;

import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetType;

import java.util.Iterator;

/**
 * @author Моклев Вячеслав
 */
public class ItSet implements Iterator<Integer>, Iterable<Integer> {
    private ISet set;
    private int element;

    @Override
    public Iterator<Integer> iterator() {
        return this;
    }

    public ItSet(ISet set) {
        this.set = set;
        element = set.getFirstElement();
    }

    @Override
    public boolean hasNext() {
        return element >= 0;
    }

    @Override
    public Integer next() {
        int result = element;
        element = set.getNextElement();
        return result;
    }

    // methods of ISet

    public boolean add(int element) {
        return set.add(element);
    }

    public void clear() {
        set.clear();
    }

    public boolean contain(int element) {
        return set.contain(element);
    }

    public int getMaxSize() {
        return set.getMaxSize();
    }

    public SetType getSetType() {
        return set.getSetType();
    }

    public int[] toArray() {
        return set.toArray();
    }

    public int getFirstElement() {
        return set.getFirstElement();
    }

    public int getNextElement() {
        return set.getNextElement();
    }

    public boolean remove(int element) {
        return set.remove(element);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public int getSize() {
        return set.getSize();
    }
}
