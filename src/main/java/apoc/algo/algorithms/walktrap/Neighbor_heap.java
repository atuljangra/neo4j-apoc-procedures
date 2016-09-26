package apoc.algo.algorithms.walktrap;

public class Neighbor_heap {
    public int size;
    private int max_size;

    Neighbor H[];   // the heap that contains a pointer to each Neighbor object stored

    private void move_up(int index) {
        while(H[index/2].delta_sigma > H[index].delta_sigma) {
            Neighbor tmp = H[index/2];
            H[index].heap_index = index/2;
            H[index/2] = H[index];
            tmp.heap_index = index;
            H[index] = tmp;
            index = index/2;
        }
    }
    private void move_down(int index) {
        while(true) {
            int min = index;
            if((2*index < size) && (H[2*index].delta_sigma < H[min].delta_sigma))
            min = 2*index;
            if(2*index+1 < size && H[2*index+1].delta_sigma < H[min].delta_sigma)
            min = 2*index+1;
            if(min != index) {
                Neighbor tmp = H[min];
                H[index].heap_index = min;
                H[min] = H[index];
                tmp.heap_index = index;
                H[index] = tmp;
                index = min;
            }
            else break;
        }
    }

    public void add(Neighbor N) {        // add a new distance
        if(size >= max_size) return;
        N.heap_index = size++;
        H[N.heap_index] = N;
        move_up(N.heap_index);
    }

    public void update(Neighbor N) {        // update a distance
        if(N.heap_index == -1) return;
        move_up(N.heap_index);
        move_down(N.heap_index);
    }

    public void remove(Neighbor N) {        // remove a distance
        if(N.heap_index == -1 || size == 0) return;
        Neighbor last_N = H[--size];
        H[N.heap_index] = last_N;
        last_N.heap_index = N.heap_index;
        move_up(last_N.heap_index);
        move_down(last_N.heap_index);
        N.heap_index = -1;
    }

    public Neighbor get_first() {        // get the first item
        if(size == 0) return null;
        else return H[0];
    }

    public long memory() {
        assert false;
        return 0;
    }

    public boolean is_empty() {
        return (size == 0);
    }

    Neighbor_heap(int max_s) {
        max_size = max_s;
        size = 0;
        H = new Neighbor[max_size];
        for (int i = 0; i < max_size; i++)
            H[i] = new Neighbor();
    }
}