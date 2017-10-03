import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

import java.util.*;

/**
 * A skip list implementation.
 * Similar to ordered trees, but in list form.
 * Achieves time complexity similar to ordered trees using a
 * probabilistic approach.
 *
 * <p>
 * Time Complexity:
 * <ul>
 *   <li>Insertion O(log n)
 *   <li>Removal O(log n)
 *   <li>Contains O(log n)
 *   <li>Random-access O(n)
 *   <li>Iteration O(n)
 * </ul>
 * </p>
 *
 * <p>Based on William Pugh:
 *   Skip Lists. A Probabilistic Alternative to Balanced Trees, 1990.</p>
 *
 * <p>By design, a skip list is a single linked list.
 *   To be able to implement a random-access add operation in a simple manner,
 *   we extended the skip list to be double linked.
 * </p>
 * @param <T> type of the elements of this list
 */
public class SkipList<T extends Comparable<T>> implements List<T> {

  private static final int LEVEL_ONE = 0;

  private static class Node<T> {
    private ArrayList<Node<T>> next;
    private ArrayList<Node<T>> prev;
    private @Nullable
    T value;

    private Node(final @Nullable T pValue, final int pLevel) {
      value = pValue;
      next = new ArrayList<>(pLevel + 1);
      prev = new ArrayList<>(pLevel + 1);
    }

    private Node(
        final @NotNull List<Node<T>> pPrev,
        final @NotNull List<Node<T>> pNext,
        final @Nullable T pValue) {
      prev = new ArrayList<>(pPrev);
      next = new ArrayList<>(pNext);
      value = pValue;
    }

    @Nullable
    Node<T> getNext(final int pLevel) {
      return next.get(pLevel);
    }

    @Nullable
    Node<T> getPrevious(final int pLevel) {
      return prev.get(pLevel);
    }

    @Nullable
    T getValue() {
      return value;
    }

    void setValue(final @Nullable T pValue) {
      value = pValue;
    }

    void setNext(final @Nullable Node<T> pNext, final int pLevel) {
      next.set(pLevel, pNext);
    }

    void setPrevious(final @Nullable Node<T> pPrev, final int pLevel) {
      prev.set(pLevel, pPrev);
    }
  }

  private static final int MAX_LEVEL = 31;
  private static final double ONE_HALF_LOG = Math.log(0.5);
  private static final Random randomGenerator = new Random();

  private final Comparator<T> comparator;
  private Node<T> head = createHead();
  private Node<T> tail = head;
  private int size = 0;

  private ArrayList<Node<T>> partialIndices = new ArrayList<>(MAX_LEVEL);

  public SkipList(@NotNull final Comparator<T> pComparator) {
    comparator = pComparator;
  }

  public SkipList() {
    comparator = Comparator.naturalOrder();
  }

  private SkipList(final Node<T> pHead, @NotNull final Comparator<T> pComparator) {
    this(pComparator);
    head = pHead;
  }

  private Node<T> createHead() {
    return new Node<>(null, MAX_LEVEL);
  }

  /**
   * Return a random level between 0 and the max level. The probability distribution is
   * logarithmic (i.e., higher values are less likely).
   */
  private int getRandomLevel() {
    double r = randomGenerator.nextDouble();
    if (r == 0) {
      return MAX_LEVEL;
    } else {
      // change logarithmic base to 0.5
      // to get log_{0.5}(r)
      return ((int) Math.round(Math.log(r) / ONE_HALF_LOG));
    }
  }

  private @Nullable
  Node<T> getClosestLessEqual(final Node<T> pStart, final int pLevel, final T pValue) {
    Node<T> currNode = pStart;
    Node<T> next = currNode.getNext(pLevel);
    while (next != null && comparator.compare(pValue, next.getValue()) > 0) {
      currNode = next;
      next = currNode.getNext(pLevel);
    }
    return next;
  }

  @Override
  public boolean add(T pT) {
    int level = getRandomLevel();
    Node<T> newNode = new Node<>(pT, level);

    Node<T> currNode = head;
    boolean newValue = true;
    for (int currLvl = MAX_LEVEL; currLvl >= 0; currLvl--) {
      Node<T> next = getClosestLessEqual(currNode, currLvl, pT);

      if (currLvl <= level) {
        newNode.setNext(next, currLvl);
        if (next != null) {
          next.setPrevious(newNode, currLvl);
        }
        currNode.setNext(newNode, currLvl);
        newNode.setPrevious(currNode, currLvl);

        if (currNode == tail) {
          tail = newNode;
        }
      }
      if (next != null) {
        int comp = comparator.compare(pT, next.getValue());
        if (comp == 0) {
          newValue = false;
        }
        currNode = next;
      }
    }

    size++;
    return newValue;
  }

  private void removeNode(final Node<T> pNode) {
    for (int currLvl = MAX_LEVEL; currLvl >= 0; currLvl--) {
      Node<T> previous = pNode.getPrevious(currLvl);
      assert previous != null;
      Node<T> next = pNode.getNext(currLvl);
      previous.setNext(next, currLvl);
      if (next != null) {
        next.setPrevious(previous, currLvl);
      } else if (pNode == tail) {
        tail = previous;
      }
    }
  }

  @Override
  public boolean remove(final @NotNull Object pO) {
    Node<T> currNode = head;
    T val = (T) pO;
    for (int currLvl = MAX_LEVEL; currLvl >= 0; currLvl--) {
      Node<T> next = getClosestLessEqual(currNode, currLvl, val);
      if (next != null) {
        currNode = next;
        if (comparator.compare(currNode.getValue(), val) == 0) {
          removeNode(currNode);
          size--;
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean contains(final Object pO) {
    Node<T> currNode = head;
    T val = (T) pO;
    for (int currLvl = MAX_LEVEL; currLvl >= 0; currLvl--) {
      Node<T> next = getClosestLessEqual(currNode, currLvl, val);
      if (next != null) {
        if (comparator.compare(next.getValue(), val) == 0) {
          return true;
        }
        currNode = next;
      }
    }
    return false;
  }

  @Override
  public Object[] toArray() {
    Object[] arr = new Object[size];
    int i = 0;
    for (T v : this) {
      arr[i] = v;
      i++;
    }
    return arr;
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    int i = 0;
    for (T v : this) {
      a[i] = (T1) v;
      i++;
    }
    return a;
  }


  @Override
  public boolean containsAll(final Collection<?> pC) {
    for (Object o : pC) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(final Collection<? extends T> pC) {
    boolean changed = false;
    for (T o : pC) {
      changed |= add(o);
    }
    return changed;
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    throw new UnsupportedOperationException("This method doesn't make sense for a sorted list");
  }

  @Override
  public boolean removeAll(final Collection<?> pC) {
    boolean changed = false;
    for (Object o : pC) {
      changed |= remove(o);
    }
    return changed;
  }

  @Override
  public boolean retainAll(final Collection<?> pC) {
    boolean changed = false;
    Node<T> currNode = head;
    while (currNode != null) {
      Node<T> next = null;
      for (int currLvl = MAX_LEVEL; currLvl >= 0; currLvl--) {
        next = currNode.getNext(currLvl);
        while (next != null && pC.contains(next.getValue())) {
          currNode.setNext(next.getNext(currLvl), currLvl);
          next.setPrevious(currNode, currLvl);
          next = currNode.getNext(currLvl);
          changed = true;
        }
      }
      currNode = next;
    }
    return changed;
  }

  @Override
  public void clear() {
    head = createHead();
  }

  private Node<T> getNode(final int pIndex) {
    if (pIndex >= size) {
      throw new IndexOutOfBoundsException("Index: " + pIndex + ", size: " + size);
    }
    int i = 0;
    Node<T> currentNode = head;
    do {
      currentNode = currentNode.getNext(LEVEL_ONE);
      assert currentNode != null;
      i++;
    } while (i <= pIndex);
    return currentNode;
  }

  @Override
  public T get(final int pIndex) {
    return getNode(pIndex).getValue();
  }


  @Override
  public @Nullable
  T set(final int pIndex, final T pElement) {
    throw new UnsupportedOperationException("Setting value by index doesn't make sense in a "
        + "sorted list");
  }

  @Override
  public void add(final int pIndex, final T pElement) {
    throw new UnsupportedOperationException("Setting value by index doesn't make sense in a "
        + "sorted list");
  }

  @Override
  public @Nullable
  T remove(final int pIndex) {
    Node<T> node = getNode(pIndex);
    for (int currLvl = MAX_LEVEL; currLvl >= 0; currLvl--) {
      Node<T> currPrev = node.getPrevious(currLvl);
      if (currPrev != null) {
        Node<T> currNext = node.getNext(currLvl);
        currPrev.setNext(currNext, currLvl);
        if (currNext != null) {
          currNext.setPrevious(currPrev, currLvl);
        }
      }
    }
    return node.getValue();
  }


  @Override
  public int indexOf(Object o) {
    int i = 0;
    for (T v : this) {
      if (Objects.equals(v, o)) {
        return i;
      }
      i++;
    }
    return -1;
  }

  @Override
  public int lastIndexOf(Object o) {
    int i = size - 1;
    Node<T> currNode = tail;
    while (currNode != null) {
      Node<T> nextNode = currNode.getPrevious(LEVEL_ONE);
      if (nextNode != null) {
        if (Objects.equals(nextNode.getValue(), o)) {
          return i;
        }
      }
      currNode = nextNode;
      i--;
    }
    return -1;
  }

  @Override
  public Iterator<T> iterator() {
    return listIterator();
  }

  @Override
  public ListIterator<T> listIterator() {
    return new ListIterator<T>() {

      private Node<T> currentNode = head;
      private int idx = 0;

      @Override
      public boolean hasNext() {
        return currentNode.getNext(LEVEL_ONE) != null;
      }

      @Override
      public @Nullable
      T next() {
        if (!hasNext()) {
          throw new IndexOutOfBoundsException();
        }
        //noinspection ConstantConditions
        currentNode = currentNode.getNext(LEVEL_ONE);
        assert currentNode != null;
        idx++;
        return currentNode.getValue();
      }

      @Override
      public boolean hasPrevious() {
        return currentNode.getPrevious(LEVEL_ONE) != null;
      }

      @Override
      public @Nullable
      T previous() {
        if (!hasPrevious()) {
          throw new IndexOutOfBoundsException();
        }
        //noinspection ConstantConditions
        currentNode = currentNode.getPrevious(LEVEL_ONE);
        assert currentNode != null;
        idx--;
        return currentNode.getValue();
      }

      @Override
      public int nextIndex() {
        return idx + 1;
      }

      @Override
      public int previousIndex() {
        return idx - 1;
      }

      @Override
      public void remove() {
        removeNode(currentNode);
      }

      @Override
      public void set(T pT) {
        throw new UnsupportedOperationException("Setting the value at a specific position "
            + "doesn't make sense in a sorted list");
      }

      @Override
      public void add(T pT) {
        throw new UnsupportedOperationException("Adding a node at a specific position doesn't "
            + "make sense in a sorted list");
      }
    };
  }

  @Override
  public ListIterator<T> listIterator(int index) {
    ListIterator<T> it = listIterator();
    for (int i = 0; i < index; i++) {
      it.next();
    }
    return it;
  }

  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException("No clear meaning in skip list");
  }
}
