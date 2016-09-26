package apoc.algo.algorithms.walktrap;

public class Probabilities {
    public static float tmp_vector1[];    //
    public static float tmp_vector2[];    //
    public static int id[];        //
    public static int vertices1[];    //
    public static int vertices2[];    //
    public static int current_id;    //

    public static Communities C;                    // pointer to all the communities
    public static int length;                        // length of the random walks


    int size;                            // number of probabilities stored
    int vertices[];                        // the vertices corresponding to the stored probabilities, 0 if all the probabilities are stored
    float P[];                            // the probabilities

    long memory() {                        // the memory (in Bytes) used by the object
        assert false;
        return 0;
    }

    double compute_distance(Probabilities P2) {   // compute the squared distance r^2 between this probability vector and P2
        double r = 0.;
        if (vertices != null) {
            if (P2.vertices != null) {  // two partial vectors
                int i = 0;
                int j = 0;
                while ((i < size) && (j < P2.size)) {
                    if (vertices[i] < P2.vertices[j]) {
                        r += P[i] * P[i];
                        i++;
                        continue;
                    }
                    if (vertices[i] > P2.vertices[j]) {
                        r += P2.P[j] * P2.P[j];
                        j++;
                        continue;
                    }
                    r += (P[i] - P2.P[j]) * (P[i] - P2.P[j]);
                    i++;
                    j++;
                }
                if (i == size) {
                    for (; j < P2.size; j++)
                        r += P2.P[j] * P2.P[j];
                } else {
                    for (; i < size; i++)
                        r += P[i] * P[i];
                }
            } else {  // P1 partial vector, P2 full vector

                int i = 0;
                for (int j = 0; j < size; j++) {
                    for (; i < vertices[j]; i++)
                        r += P2.P[i] * P2.P[i];
                    r += (P[j] - P2.P[i]) * (P[j] - P2.P[i]);
                    i++;
                }
                for (; i < P2.size; i++)
                    r += P2.P[i] * P2.P[i];
            }
        } else {
            if (P2.vertices != null) {  // P1 full vector, P2 partial vector
                int i = 0;
                for (int j = 0; j < P2.size; j++) {
                    for (; i < P2.vertices[j]; i++)
                        r += P[i] * P[i];
                    r += (P[i] - P2.P[j]) * (P[i] - P2.P[j]);
                    i++;
                }
                for (; i < size; i++)
                    r += P[i] * P[i];
            } else {  // two full vectors
                for (int i = 0; i < size; i++)
                    r += (P[i] - P2.P[i]) * (P[i] - P2.P[i]);
            }
        }
        return r;


    }

    Probabilities(int community) {                    // compute the probability vector of a community
        Graph G = C.G;
        int nb_vertices1 = 0;
        int nb_vertices2 = 0;

        float initial_proba = 1.f / (float) (C.communities[community].size);
        int last = C.members[C.communities[community].last_member];
        for (int m = C.communities[community].first_member; m != last; m = C.members[m]) {
            tmp_vector1[m] = initial_proba;
            vertices1[nb_vertices1++] = m;
        }

        for (int t = 0; t < length; t++) {
            current_id++;
            if (nb_vertices1 > (G.nb_vertices / 2)) {
                nb_vertices2 = G.nb_vertices;
                for (int i = 0; i < G.nb_vertices; i++)
                    tmp_vector2[i] = 0.f;
                if (nb_vertices1 == G.nb_vertices) {
                    for (int i = 0; i < G.nb_vertices; i++) {
                        float proba = tmp_vector1[i] / G.vertices[i].total_weight;
                        for (int j = 0; j < G.vertices[i].degree; j++)
                            tmp_vector2[G.vertices[i].edges[j].neighbor] += proba * G.vertices[i].edges[j].weight;
                    }
                } else {
                    for (int i = 0; i < nb_vertices1; i++) {
                        int v1 = vertices1[i];
                        float proba = tmp_vector1[v1] / G.vertices[v1].total_weight;
                        for (int j = 0; j < G.vertices[v1].degree; j++)
                            tmp_vector2[G.vertices[v1].edges[j].neighbor] += proba * G.vertices[v1].edges[j].weight;
                    }
                }
            } else {
                nb_vertices2 = 0;
                for (int i = 0; i < nb_vertices1; i++) {
                    int v1 = vertices1[i];
                    float proba = tmp_vector1[v1] / G.vertices[v1].total_weight;
                    for (int j = 0; j < G.vertices[v1].degree; j++) {
                        int v2 = G.vertices[v1].edges[j].neighbor;
                        if (id[v2] == current_id)
                            tmp_vector2[v2] += proba * G.vertices[v1].edges[j].weight;
                        else {
                            tmp_vector2[v2] = proba * G.vertices[v1].edges[j].weight;
                            id[v2] = current_id;
                            vertices2[nb_vertices2++] = v2;
                        }
                    }
                }
            }
            float tmp[] = tmp_vector2;
            tmp_vector2 = tmp_vector1;
            tmp_vector1 = tmp;

            int tmp2[] = vertices2;
            vertices2 = vertices1;
            vertices1 = tmp2;

            nb_vertices1 = nb_vertices2;
        }

        if (nb_vertices1 > (G.nb_vertices / 2)) {
            P = new float[G.nb_vertices];
            size = G.nb_vertices;
            vertices = null;
            if (nb_vertices1 == G.nb_vertices) {
                for (int i = 0; i < G.nb_vertices; i++)
                    P[i] = (float) (tmp_vector1[i] / (float) Math.sqrt(G.vertices[i].total_weight));
            } else {
                for (int i = 0; i < G.nb_vertices; i++)
                    P[i] = 0.f;
                for (int i = 0; i < nb_vertices1; i++)
                    P[vertices1[i]] = tmp_vector1[vertices1[i]] / (float) Math.sqrt(G.vertices[vertices1[i]].total_weight);
            }
        } else {
            P = new float[nb_vertices1];
            size = nb_vertices1;
            vertices = new int[nb_vertices1];
            int j = 0;
            for (int i = 0; i < G.nb_vertices; i++) {
                if (id[i] == current_id) {
                    P[j] = tmp_vector1[i] / (float) Math.sqrt(G.vertices[i].total_weight);
                    vertices[j] = i;
                    j++;
                }
            }
        }
        C.memory_used += memory();
    }

    // merge the probability vectors of two communities in a new one
    // the two communities must have their probability vectors stored
    Probabilities(int community1, int community2) {
        // The two following probability vectors must exist.
        // Do not call this function if it is not the case.
        Probabilities P1 = C.communities[community1].P;
        Probabilities P2 = C.communities[community2].P;

        float w1 = (float) (C.communities[community1].size) / (float) (C.communities[community1].size + C.communities[community2].size);
        float w2 = (float) (C.communities[community2].size) / (float) (C.communities[community1].size + C.communities[community2].size);


        if (P1.size == C.G.nb_vertices) {
            P = new float[C.G.nb_vertices];
            size = C.G.nb_vertices;
            vertices = null;

            if (P2.size == C.G.nb_vertices) {    // two full vectors
                for (int i = 0; i < C.G.nb_vertices; i++)
                    P[i] = P1.P[i] * w1 + P2.P[i] * w2;
            } else {  // P1 full vector, P2 partial vector
                int j = 0;
                for (int i = 0; i < P2.size; i++) {
                    for (; j < P2.vertices[i]; j++)
                        P[j] = P1.P[j] * w1;
                    P[j] = P1.P[j] * w1 + P2.P[i] * w2;
                    j++;
                }
                for (; j < C.G.nb_vertices; j++)
                    P[j] = P1.P[j] * w1;
            }
        } else {
            if (P2.size == C.G.nb_vertices) { // P1 partial vector, P2 full vector
                P = new float[C.G.nb_vertices];
                size = C.G.nb_vertices;
                vertices = null;

                int j = 0;
                for (int i = 0; i < P1.size; i++) {
                    for (; j < P1.vertices[i]; j++)
                        P[j] = P2.P[j] * w2;
                    P[j] = P1.P[i] * w1 + P2.P[j] * w2;
                    j++;
                }
                for (; j < C.G.nb_vertices; j++)
                    P[j] = P2.P[j] * w2;
            } else {  // two partial vectors
                int i = 0;
                int j = 0;
                int nb_vertices1 = 0;
                while ((i < P1.size) && (j < P2.size)) {
                    if (P1.vertices[i] < P2.vertices[j]) {
                        tmp_vector1[P1.vertices[i]] = P1.P[i] * w1;
                        vertices1[nb_vertices1++] = P1.vertices[i];
                        i++;
                        continue;
                    }
                    if (P1.vertices[i] > P2.vertices[j]) {
                        tmp_vector1[P2.vertices[j]] = P2.P[j] * w2;
                        vertices1[nb_vertices1++] = P2.vertices[j];
                        j++;
                        continue;
                    }
                    tmp_vector1[P1.vertices[i]] = P1.P[i] * w1 + P2.P[j] * w2;
                    vertices1[nb_vertices1++] = P1.vertices[i];
                    i++;
                    j++;
                }
                if (i == P1.size) {
                    for (; j < P2.size; j++) {
                        tmp_vector1[P2.vertices[j]] = P2.P[j] * w2;
                        vertices1[nb_vertices1++] = P2.vertices[j];
                    }
                } else {
                    for (; i < P1.size; i++) {
                        tmp_vector1[P1.vertices[i]] = P1.P[i] * w1;
                        vertices1[nb_vertices1++] = P1.vertices[i];
                    }
                }

                if (nb_vertices1 > (C.G.nb_vertices / 2)) {
                    P = new float[C.G.nb_vertices];
                    size = C.G.nb_vertices;
                    vertices = null;
                    for (int k = 0; k < C.G.nb_vertices; k++)
                        P[k] = 0.f;
                    for (int k = 0; k < nb_vertices1; k++)
                        P[vertices1[k]] = tmp_vector1[vertices1[k]];
                } else {
                    P = new float[nb_vertices1];
                    size = nb_vertices1;
                    vertices = new int[nb_vertices1];
                    for (int k = 0; k < nb_vertices1; k++) {
                        vertices[k] = vertices1[k];
                        P[k] = tmp_vector1[vertices1[k]];
                    }
                }
            }
        }

        C.memory_used += memory();
    }
}
