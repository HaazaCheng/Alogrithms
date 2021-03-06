package com.hazza.algorithms.graph.Dijkstra;

import com.hazza.algorithms.utils.ds.Edge;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: HazzaCheng
 * Contact: hazzacheng@gmail.com
 * Date: 18-2-13
 * Time: 下午8:03
 */
public class FindShortestPathTest {
    @Test
    public void testExisted() {
        Edge[] edges = new Edge[9];
        edges[0] = new Edge(1, 2, 1);
        edges[1] = new Edge(1, 3, 12);
        edges[2] = new Edge(2, 3, 9);
        edges[3] = new Edge(2, 4, 3);
        edges[4] = new Edge(3, 5, 5);
        edges[5] = new Edge(4, 3, 4);
        edges[6] = new Edge(4, 5, 13);
        edges[7] = new Edge(4, 6, 15);
        edges[8] = new Edge(5, 6, 4);

        FindShortestPath.dijkstra(6, 1, edges);

        assertEquals(0, FindShortestPath.getDistanceFromSource(1));
        assertEquals(1, FindShortestPath.getDistanceFromSource(2));
        assertEquals(8, FindShortestPath.getDistanceFromSource(3));
        assertEquals(4, FindShortestPath.getDistanceFromSource(4));
        assertEquals(13, FindShortestPath.getDistanceFromSource(5));
        assertEquals(17, FindShortestPath.getDistanceFromSource(6));

        for (int i = 1; i <= 6 ; i++) FindShortestPath.printPath(1, i);
    }

    @Test (expected = RuntimeException.class)
    public void nonExisted() {
        Edge[] edges = new Edge[1];
        edges[0] = new Edge(2, 3, 2);

        FindShortestPath.dijkstra(3, 1, edges);
    }
}
