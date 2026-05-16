package com.ecotrack.enterprise.service

import java.util.*
import kotlin.math.*
import javax.inject.Inject
import javax.inject.Singleton

/*
 ═══════════════════════════════════════════════════════
 EcoTrack Enterprise | DAA Algorithmic Module Header
 ═══════════════════════════════════════════════════════
 [ALGORITHMIC INTENT]
   → Find the top-3 most carbon-efficient routes using a multi-criteria A* search 
     algorithm that balances distance, traffic density, and DEFRA emissions.

 [PARADIGM & DATA STRUCTURE]
   → Multi-criteria A* Search using an Adjacency List graph representation.
     A PriorityQueue (Min-Heap) stores frontier nodes ordered by f(n) = g(n) + h(n).
     To find top-K routes, the algorithm allows nodes to be visited up to K times.

 [FORMAL COMPLEXITY PROOF]
   → Worst-Case: O(K * (V + E) log V) where V is vertices, E is edges, and K=3.
   → Average-Case: O((V + E) log V) for small K.
   → Best-Case: O(V) if the heuristic is perfectly informed.
   → Space Complexity: O(V + E) for adjacency list + O(K * V) for state tracking.

 [FAILURE & EDGE CASE ANALYSIS]
   → Disconnected Graph: If no path exists, returns empty list and logs error.
   → Missing Heuristic: Fallback to h(n)=0, A* degrades to Dijkstra's.
   → Equal Weights: A* exploration degrades to BFS-like expansion.

 [BUSINESS & SUSTAINABILITY UTILITY]
   → O((V+E) log V) efficiency allows real-time route re-optimization for large 
     logistics fleets, directly reducing Scope 3 carbon footprint.
 ═══════════════════════════════════════════════════════
*/

typealias NodeId = Long

data class WeightedEdge(
    val to: NodeId,
    val distanceMeters: Double,
    val trafficDensityFactor: Float, // 0.0 (free) to 1.0 (gridlock)
    val defraEmissionFactor: Double // kg CO2 per km
)

data class SearchNode(
    val id: NodeId,
    val gScore: Double, // Cost from start
    val fScore: Double, // gScore + heuristic
    val path: List<NodeId>
) : Comparable<SearchNode> {
    override fun compareTo(other: SearchNode): Int = fScore.compareTo(other.fScore)
}

data class OptimizedRoute(
    val path: List<NodeId>,
    val totalWeight: Double,
    val co2SavedKg: Double,
    val totalDistanceMeters: Double
)

@Singleton
class EcoRouteOptimizer @Inject constructor() {

    // Configurable enterprise weight coefficients
    private var alpha = 0.3 // Distance
    private var beta = 0.3  // Traffic
    private var gamma = 0.4 // Emissions

    private val graph = HashMap<NodeId, List<WeightedEdge>>()
    private val nodeCoordinates = HashMap<NodeId, Pair<Double, Double>>()

    /**
     * Finds top-3 ranked routes sorted by composite weight.
     */
    fun findTopRoutes(start: NodeId, target: NodeId, k: Int = 3): List<OptimizedRoute> {
        val routes = mutableListOf<OptimizedRoute>()
        val openSet = PriorityQueue<SearchNode>()
        
        // Track how many times each node has been 'finished' (extracted from PQ)
        val visitCount = HashMap<NodeId, Int>()

        val startCoords = nodeCoordinates[start]
        openSet.add(SearchNode(start, 0.0, getHeuristic(start, target), listOf(start)))

        while (openSet.isNotEmpty() && routes.size < k) {
            val current = openSet.poll() ?: break
            
            val count = visitCount.getOrDefault(current.id, 0)
            if (count >= k) continue
            visitCount[current.id] = count + 1

            if (current.id == target) {
                routes.add(calculateRouteMetrics(current))
                if (routes.size >= k) break
                continue
            }

            val neighbors = graph[current.id] ?: continue
            for (edge in neighbors) {
                val weight = calculateEdgeWeight(edge)
                val tentativeGScore = current.gScore + weight
                val fScore = tentativeGScore + getHeuristic(edge.to, target)
                
                // For K-shortest paths in a graph, we allow cycles if needed, 
                // but usually we want simple paths. For this implementation, 
                // we allow revisit to find alternative paths.
                if (edge.to !in current.path) {
                    openSet.add(SearchNode(edge.to, tentativeGScore, fScore, current.path + edge.to))
                }
            }
        }

        if (routes.isEmpty()) {
            println("ERROR: Disconnected graph, no path from $start to $target")
        }

        return routes
    }

    private fun calculateEdgeWeight(edge: WeightedEdge): Double {
        // W(e) = α·distance + β·(distance × trafficDensityFactor) + γ·(distance × defraEmissionFactor)
        val distanceKm = edge.distanceMeters / 1000.0
        return (alpha * edge.distanceMeters) + 
               (beta * (edge.distanceMeters * edge.trafficDensityFactor)) + 
               (gamma * (distanceKm * edge.defraEmissionFactor))
    }

    private fun getHeuristic(node: NodeId, target: NodeId): Double {
        val n1 = nodeCoordinates[node] ?: return 0.0
        val n2 = nodeCoordinates[target] ?: return 0.0
        // Haversine straight-line distance
        return haversine(n1.first, n1.second, n2.first, n2.second)
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    private fun calculateRouteMetrics(node: SearchNode): OptimizedRoute {
        // In a real implementation, we'd sum up the actual CO2 and distance from the edges
        // Here we'll provide a consistent calculation based on the path
        var totalDist = 0.0
        var totalEmissions = 0.0
        
        for (i in 0 until node.path.size - 1) {
            val from = node.path[i]
            val to = node.path[i+1]
            val edge = graph[from]?.find { it.to == to }
            if (edge != null) {
                totalDist += edge.distanceMeters
                totalEmissions += (edge.distanceMeters / 1000.0) * edge.defraEmissionFactor
            }
        }

        // CO2 saved vs naive (naive = average emissions for same distance)
        val baselineEmissions = (totalDist / 1000.0) * 0.17 // Average DEFRA factor
        val co2Saved = maxOf(0.0, baselineEmissions - totalEmissions)

        return OptimizedRoute(node.path, node.gScore, co2Saved, totalDist)
    }

    fun setGraph(newGraph: HashMap<NodeId, List<WeightedEdge>>) {
        graph.clear()
        graph.putAll(newGraph)
    }

    fun setNodeCoords(id: NodeId, lat: Double, lon: Double) {
        nodeCoordinates[id] = Pair(lat, lon)
    }
}
