/*
 * Copyright (C) 2020 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

internal class DirectedAcyclicGraph<N>(
  private val nodes: Iterable<N>,
  private val edges: (N) -> Iterable<N>,
) {
  /** Nodes which have no outgoing edges. */
  private val seeds = nodes.filterNot { edges(it).iterator().hasNext() }

  private fun incomingEdges(node: N) = nodes.filter { node in edges(it) }

  fun disjointGraphs(): Set<Set<N>> {
    val graphs = mutableSetOf<Set<N>>()
    for (root in seeds) {
      val reachableNames = mutableSetOf<N>()
      val visitQueue = ArrayDeque<N>().apply { add(root) }
      while (visitQueue.isNotEmpty()) {
        val visitName = visitQueue.removeFirst()
        reachableNames += visitName

        for (dependency in edges(visitName) + incomingEdges(visitName)) {
          if (dependency !in reachableNames && dependency !in visitQueue) {
            visitQueue += dependency
          }
        }
      }
      graphs += reachableNames
    }
    return graphs
  }

  fun topologicalOrder(): List<N> {
    val incomingEdges = mutableMapOf<N, Int>()
    for (vertex in nodes) {
      if (vertex !in incomingEdges) {
        incomingEdges[vertex] = 0
      }
      for (edge in edges(vertex)) {
        incomingEdges[edge] = (incomingEdges[edge] ?: 0) + 1
      }
    }

    val queue = ArrayDeque<N>()
    for ((vertex, edges) in incomingEdges) {
      if (edges == 0) queue += vertex
    }

    val result = mutableListOf<N>()

    while (queue.isNotEmpty()) {
      val vertex = queue.removeFirst()
      result += vertex

      for (edge in edges(vertex)) {
        incomingEdges[edge] = (incomingEdges[edge] ?: 0) - 1
        if (incomingEdges[edge] == 0) {
          queue += edge
        }
      }
    }

    check(result.size == incomingEdges.size) {
      "Graph contains a cycle, topological sort not possible!"
    }

    return result
  }

  fun transitiveNodes(node: N): Set<N> {
    val nodes = mutableSetOf<N>()
    val queue = ArrayDeque<N>().apply { add(node) }
    while (queue.isNotEmpty()) {
      val elements = edges(queue.removeFirst())
      nodes.addAll(elements)
      queue.addAll(elements)
    }
    return nodes
  }
}
