package apoc.algo.algorithms.walktrap;

public class Min_delta_sigma_heap {

    private int size;
    private int max_size;

    private int H[];   // the heap that contains the number of each community
    private int I[];   // the index of each community in the heap (-1 = not stored)

    private void move_up(int index) {
        while(delta_sigma[H[index/2]] < delta_sigma[H[index]]) {
            int tmp = H[index/2];
            I[H[index]] = index/2;
            H[index/2] = H[index];
            I[tmp] = index;
            H[index] = tmp;
            index = index/2;
        }

    }
    private void move_down(int index) {
        while(true) {
            int max = index;
            if(2*index < size && delta_sigma[H[2*index]] > delta_sigma[H[max]])
                max = 2*index;
            if(2*index+1 < size && delta_sigma[H[2*index+1]] > delta_sigma[H[max]])
                max = 2*index+1;
            if(max != index) {
                int tmp = H[max];
                I[H[index]] = max;
                H[max] = H[index];
                I[tmp] = index;
                H[index] = tmp;
                index = max;
            }
            else break;
        }
    }

    public int get_max_community() {                // return the community with the maximal delta_sigma
        if(size == 0) return -1;
        else return H[0];
    }

    public void remove_community(int community) {            // remove a community;
        if(I[community] == -1 || size == 0) return;
        int last_community = H[--size];
        H[I[community]] = last_community;
        I[last_community] = I[community];
        move_up(I[last_community]);
        move_down(I[last_community]);
        I[community] = -1;
    }

    public void update(int community) {                // update (or insert if necessary) the community
        if(community < 0 || community >= max_size) return;
        if(I[community] == -1) {
            I[community] = size++;
            H[I[community]] = community;
        }
        move_up(I[community]);
        move_down(I[community]);
    }

    public long memory() {                    // the memory used in Bytes.
        assert false;
        return 0;
    }

    public boolean is_empty() {
        if ((size == 0)) return true;
        else return false;
    }

    public float delta_sigma[];                  // the delta_sigma of the stored communities

    public Min_delta_sigma_heap(int max_s) {
        max_size = max_s;
        size = 0;
        H = new int[max_s];
        I = new int[max_s];
        delta_sigma = new float[max_s];
        for(int i = 0; i < max_size; i++) {
            I[i] = -1;
            delta_sigma[i] = 1.f;
        }

    }
};
