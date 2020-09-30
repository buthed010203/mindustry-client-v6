package mindustry.client.navigation

import arc.math.Mathf
import arc.struct.Seq
import mindustry.Vars
import mindustry.client.navigationimport.TurretPathfindingEntity
import mindustry.game.Team
import mindustry.world.blocks.defense.turrets.Turret
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

// Taken from http://www.codebytes.in/2015/02/a-shortest-path-finding-algorithm.html
// and modified
object AStar {
    const val DIAGONAL_COST = 14
    private const val V_H_COST = 10

    //Blocked cells are just null Cell values in grid
    var grid = Array(5) { arrayOfNulls<Cell>(5) }
    var open = PriorityQueue<Cell>()
    lateinit var closed: Array<BooleanArray>
    private lateinit var costly: Array<BooleanArray>
    private var startI = 0
    private var startJ = 0
    private var endI = 0
    private var endJ = 0
    private var block = false

    fun setBlocked(i: Int, j: Int) {
        grid[i][j] = null
    }

    private fun setStartCell(i: Int, j: Int) {
        startI = i
        startJ = j
    }

    fun setEndCell(i: Int, j: Int) {
        endI = i
        endJ = j
    }

    private fun checkAndUpdateCost(current: Cell?, t: Cell?, cost: Int) {
        if (t == null || closed[t.i][t.j]) return
        val tFinalCost = t.heuristicCost + cost
        //        if(closed[t.i][t.j]){
//            t_final_cost *= 100;
//        }
        val inOpen = open.contains(t)
        if (!inOpen || tFinalCost < t.finalCost) {
            t.finalCost = tFinalCost
            t.parent = current
            if (!inOpen) open.add(t)
        }
    }

    private fun aStarSearch() {
//        System.out.println(grid.length);
//        System.out.println(grid[0].length);
//        System.out.println(startI);
//        System.out.println(startJ);
//        System.out.println(Seqs.deepToString(grid));
//        System.out.println(Seqs.toString(grid[startI]));
        //add the start location to open list.
        endI = Mathf.clamp(endI, 0, grid.size - 1)
        endJ = Mathf.clamp(endJ, 0, grid[0].size - 1)
        startI = Mathf.clamp(startI, 0, grid.size - 1)
        startJ = Mathf.clamp(startJ, 0, grid[0].size - 1)
        open.add(grid[startI][startJ])
        var current: Cell?
        //        System.out.println(Seqs.deepToString(costly));
        while (true) {
            current = open.poll()
            if (current == null) break
            //            if(costly[current.i][current.j] && block){
//                break;
//            }
            closed[current.i][current.j] = true
            if (current == grid[endI][endJ]) {
                return
            }
            var t: Cell?
            val multiplier: Int = if (costly[current.i][current.j]) {
                500
            } else {
                1
            }
            if (current.i - 1 >= 0) {
                t = grid[current.i - 1][current.j]
                checkAndUpdateCost(current, t, (current.finalCost + V_H_COST) * multiplier)
                if (current.j - 1 >= 0) {
                    t = grid[current.i - 1][current.j - 1]
                    checkAndUpdateCost(current, t, (current.finalCost + DIAGONAL_COST) * multiplier)
                }
                if (current.j + 1 < grid[0].size) {
                    t = grid[current.i - 1][current.j + 1]
                    checkAndUpdateCost(current, t, (current.finalCost + DIAGONAL_COST) * multiplier)
                }
            }
            if (current.j - 1 >= 0) {
                t = grid[current.i][current.j - 1]
                checkAndUpdateCost(current, t, (current.finalCost + V_H_COST) * multiplier)
            }
            if (current.j + 1 < grid[0].size) {
                t = grid[current.i][current.j + 1]
                checkAndUpdateCost(current, t, (current.finalCost + V_H_COST) * multiplier)
            }
            if (current.i + 1 < grid.size) {
                t = grid[current.i + 1][current.j]
                checkAndUpdateCost(current, t, (current.finalCost + V_H_COST) * multiplier)
                if (current.j - 1 >= 0) {
                    t = grid[current.i + 1][current.j - 1]
                    checkAndUpdateCost(current, t, (current.finalCost + DIAGONAL_COST) * multiplier)
                }
                if (current.j + 1 < grid[0].size) {
                    t = grid[current.i + 1][current.j + 1]
                    checkAndUpdateCost(current, t, (current.finalCost + DIAGONAL_COST) * multiplier)
                }
            }
        }
    }

    /*
    Params :
    tCase = test case No.
    x, y = Board's dimensions
    si, sj = start location's x and y coordinates
    ei, ej = end location's x and y coordinates
    int[][] blocked = Seq containing inaccessible cell coordinates
    */
    fun test(tCase: Int, x: Int, y: Int, si: Int, sj: Int, ei: Int, ej: Int, blocked: Array<IntArray>) {
        println("\n\nmindustry.client.utils.Test Case #$tCase")
        //Reset
        grid = Array(x) { arrayOfNulls(y) }
        closed = Array(x) { BooleanArray(y) }
        open = PriorityQueue { o1: Any?, o2: Any? ->
            val c1 = o1 as Cell?
            val c2 = o2 as Cell?
            c1!!.finalCost.compareTo(c2!!.finalCost)
        }
        //Set start position
        setStartCell(si, sj) //Setting to 0,0 by default. Will be useful for the UI part

        //Set End Location
        setEndCell(ei, ej)
        for (i in 0 until x) {
            for (j in 0 until y) {
                grid[i][j] = Cell(i, j)
                grid[i][j]!!.heuristicCost = abs(i - endI) + abs(j - endJ)
                //                  System.out.print(grid[i][j].heuristicCost+" ");
            }
            //              System.out.println();
        }
        grid[si][sj]!!.finalCost = 0

        /*
             Set blocked cells. Simply set the cell values to null
             for blocked cells.
           */for (ints in blocked) {
            setBlocked(ints[0], ints[1])
        }

        //Display initial map
        println("Grid: ")
        for (i in 0 until x) {
            for (j in 0 until y) {
                if (i == si && j == sj) print("SO  ") //Source
                else if (i == ei && j == ej) print("DE  ") //Destination
                else if (grid[i][j] != null) System.out.printf("%-3d ", 0) else print("BL  ")
            }
            println()
        }
        println()
        aStarSearch()
        println("\nScores for cells: ")
        for (i in 0 until x) {
            for (j in 0 until x) {
                if (grid[i][j] != null) System.out.printf("%-3d ", grid[i][j]!!.finalCost) else print("BL  ")
            }
            println()
        }
        println()
        if (closed[endI][endJ]) {
            //Trace back the path
            println("Path: ")
            var current = grid[endI][endJ]
            print(current)
            while (current!!.parent != null) {
                print(" -> " + current.parent)
                current = current.parent
            }
            println()
        } else println("No possible path")
    }

    fun findPathTurretsDropZone(turrets: Seq<Turret.TurretBuild?>, playerX: Float, playerY: Float, targetX: Float, targetY: Float, width: Int, height: Int, team: Team, dropZones: Seq<TurretPathfindingEntity?>): Seq<IntArray>? {
        val resolution = 2 // The resolution of the map is divided by this value
        val pathfindingEntities: Seq<TurretPathfindingEntity?> = Seq<TurretPathfindingEntity?>()
        for (turretEntity in turrets) {
            if (turretEntity != null) {
                if (turretEntity.team === team) {
                    continue
                }
            }
            val flying: Boolean = Vars.player.unit().isFlying
            val targetsAir: Boolean = (turretEntity?.block as Turret).targetAir
            val targetsGround: Boolean = (turretEntity.block as Turret).targetGround
            if (flying && !targetsAir) {
                continue
            }
            if (!flying && !targetsGround) {
                continue
            }
            pathfindingEntities.add(TurretPathfindingEntity(turretEntity.tileX() / resolution, turretEntity.tileY() / resolution, (turretEntity.block as Turret).range / (8 * resolution)))
        }
        for (zone in dropZones) {
            if (zone != null) {
                pathfindingEntities.add(TurretPathfindingEntity(zone.x / resolution, zone.y / resolution, zone.range / resolution))
            }
        }
        block = true
        var path: Seq<IntArray>? = findPath(pathfindingEntities, playerX / resolution, playerY / resolution, targetX / resolution, targetY / resolution, width / resolution, height / resolution)
        val output: Seq<IntArray> = Seq<IntArray>()
        if (path == null) {
            block = false
            // Path blocked, retrying with cost
            path = findPath(pathfindingEntities, playerX / resolution, playerY / resolution, targetX / resolution, targetY / resolution, width / resolution, height / resolution)
        }
        if (path == null) {
            return null
        }
        for (item in path) {
            output.add(intArrayOf(item[0] * resolution, item[1] * resolution))
        }
        return output
    }

    private fun findPath(turrets: Seq<TurretPathfindingEntity?>, startX: Float, startY: Float, endX: Float, endY: Float, width: Int, height: Int): Seq<IntArray>? {
        val playerX = Mathf.clamp(startX, 0f, width * 8.toFloat())
        val playerY = Mathf.clamp(startY, 0f, height * 8.toFloat())
        val targetX = Mathf.clamp(endX, 0f, width * 8.toFloat())
        val targetY = Mathf.clamp(endY, 0f, height * 8.toFloat())
        if (turrets.size == 0) {
            val out: Seq<IntArray> = Seq<IntArray>()
            out.add(intArrayOf(targetX.toInt() / 8, targetY.toInt() / 8))
            return out
        }
        //        long startTime = System.currentTimeMillis();
        val blocked2 = ArrayList<IntArray>()
        for (turret in turrets) {
//            if(turret.getTeam() == player.getTeam()){
//                continue;
//            }
            if (turret == null) {
                continue
            }
            val range: Float = turret.range
            val x: Float = turret.x.toFloat()
            val y: Float = turret.y.toFloat()
            var colNum = 0
            while (colNum <= width - 1) {
                if (colNum > x + range) {
                    colNum += 1
                    continue
                }
                if (colNum < x - range) {
                    colNum += 1
                    continue
                }
                var blockNum = 0
                while (blockNum <= height - 1) {
                    if (blockNum > y + range) {
                        blockNum += 1
                        continue
                    }
                    if (blockNum < y - range) {
                        blockNum += 1
                        continue
                    }
                    if (Mathf.sqrt(Mathf.pow(x - colNum, 2f) + Mathf.pow(y - blockNum, 2f)) < range) {
                        blocked2.add(intArrayOf(colNum, blockNum))
                    }
                    blockNum += 1
                }
                colNum += 1
            }
        }
        val blocked = Array(blocked2.size) { IntArray(2) }
        var b = 0
        while (b <= blocked2.size - 1) {
            blocked[b] = blocked2[b]
            b += 1
        }
        //Reset
        val px = playerX.toInt() / 8
        val py = playerY.toInt() / 8
        val ex = targetX.toInt() / 8
        val ey = targetY.toInt() / 8
        grid = emptyArray()
        grid = Array(width) { arrayOfNulls(height) }
        closed = emptyArray()
        closed = Array(width) { BooleanArray(height) }
        costly = emptyArray()
        costly = Array(width) { BooleanArray(height) }
        open.clear()
        open = PriorityQueue { o1: Any?, o2: Any? ->
            val c1 = o1 as Cell?
            val c2 = o2 as Cell?
            c1!!.finalCost.compareTo(c2!!.finalCost)
        }
        //Set start position
        setStartCell(px, py)
        if (costly[px][py]) {
            costly[px][py] = false
        }

        //Set End Location
        setEndCell(ex, ey)
        for (i in 0 until width) {
            for (j in 0 until height) {
                grid[i][j] = Cell(i, j)
                grid[i][j]!!.heuristicCost = abs(i - endI) + abs(j - endJ)
                //                  System.out.print(grid[i][j].heuristicCost+" ");
            }
            //              System.out.println();
        }
        grid[px][py]!!.finalCost = 0

        /*
             Set blocked cells. Simply set the cell values to null
             for blocked cells.
           */for (ints in blocked) {
            if (block) {
                setBlocked(ints[0], ints[1])
            } else {
                costly[ints[0]][ints[1]] = true
            }
        }

        //Display initial map
//        System.out.println("Grid: ");
//        for(int i = 0; i < width; ++i){
//            for(int j = 0; j < height; ++j){
//                if(i == px && j == py) System.out.print("SO  "); //Source
//                else if(i == ex && j == ey) System.out.print("DE  ");  //Destination
//                else if(grid[i][j] != null) System.out.printf("%-3d ", 0);
//                else System.out.print("BL  ");
//            }
//            System.out.println();
//        }
//        System.out.println();
//        System.out.println("eifwief");
//        System.out.println(grid.length);
//        System.out.println(grid[0].length);
        aStarSearch()
        //        System.out.println("\nScores for cells: ");
//        for(int i = 0; i < width; ++i){
//            for(int j = 0; j < height; ++j){
//                if(grid[i][j] != null) System.out.printf("%-3d ", grid[i][j].finalCost);
//                else System.out.print("BL  ");
//            }
//            System.out.println();
//        }
//        System.out.println();
        return if (closed[endI][endJ]) {
            val points: Seq<IntArray> = Seq<IntArray>()
            //Trace back the path
//            System.out.println("Path: ");
            var current = grid[endI][endJ]
            while (current!!.parent != null) {
//                System.out.print(" -> " + current.parent);
                points.add(intArrayOf(current.parent!!.i, current.parent!!.j))
                current = current.parent
            }
            //            System.out.println("Time taken = " + (System.currentTimeMillis() - startTime) + " ms");
            points
            //            System.out.println();
        } else {
//            System.out.println("Time taken = " + (System.currentTimeMillis() - startTime) + " ms, no path found");
            null
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
//        test(1, 5, 5, 0, 0, 3, 2, new int[][]{{0,4},{2,2},{3,1},{3,3}});
//        test(2, 5, 5, 0, 0, 4, 4, new int[][]{{0,4},{2,2},{3,1},{3,3}});
//        test(3, 7, 7, 2, 1, 5, 4, new int[][]{{4,1},{4,3},{5,3},{2,3}});
//
//        test(1, 5, 5, 0, 0, 4, 4, new int[][]{{3,4},{3,3},{4,3}});
        val r = Random()
        val turrets: Seq<TurretPathfindingEntity?> = Seq<TurretPathfindingEntity?>()
        var i = 0
        while (i < 10) {
            val x = (r.nextFloat() * 50).toInt()
            val y = (r.nextFloat() * 50).toInt()
            turrets.add(TurretPathfindingEntity(x, y, 5f))
            i += 1
        }
        findPath(turrets, 8f, 16f, 32f * 8, 48f * 8, 50, 50)
    }

    class Cell(var i: Int, var j: Int) {
        var heuristicCost = 0 //Heuristic cost
        var finalCost = 0 //G+H
        var parent: Cell? = null
        override fun toString(): String {
            return "[$i, $j]"
        }
    }
}