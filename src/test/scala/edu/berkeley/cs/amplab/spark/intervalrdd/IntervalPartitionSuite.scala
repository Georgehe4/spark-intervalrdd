/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
F* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.berkeley.cs.amplab.spark.intervalrdd

import org.bdgenomics.adam.models.ReferenceRegion
import com.github.akmorrow13.intervaltree._
import org.scalatest._
import org.apache.spark.rdd.RDD
import org.scalatest.FunSuite
import org.scalatest.Matchers

class IntervalPartitionSuite extends FunSuite  {

	test("create new partition") {
		val region = new ReferenceRegion("chr", 0, 100)
		var partition: IntervalPartition[Long, Long] = new IntervalPartition[Long, Long]()
		assert(partition != null)
	}

	test("create partition from iterator") {
		val chr = "chr1"
		val region1: ReferenceRegion = new ReferenceRegion(chr, 0L, 99L)
		val region2: ReferenceRegion = new ReferenceRegion(chr,100L, 199L)

		val read1 = (1L,2L)
		val read2 = (1L,4L)


		val iter = Iterator((region1, read1), (region2, read1))
		val partition = IntervalPartition(iter)
		assert(partition != null)
	}

	test("get values from iterator-created partition") {

		val chr1 = "chr1"
		val region1: ReferenceRegion = new ReferenceRegion(chr1, 0L, 99L)
		val region2: ReferenceRegion = new ReferenceRegion(chr1,100L, 199L)

		val read1 = (1L,2L)
		val read2 = (1L,500L)
		val read3 = (2L, 2L)
		val read4 =  (2L, 500L)

		val iter = Iterator((region1, read1), (region2, read2), (region1, read3), (region2, read4))
		val partition = IntervalPartition(iter)

		var results: List[(Long, Long)] = partition.get(region1).toList
		results = results ++ partition.get(region2).toList

		assert(results.contains(read1))
		assert(results.contains(read2))
		assert(results.contains(read3))
		assert(results.contains(read4))
}

	test("put some for iterator of intervals and key-values") {

		val chr1 = "chr1"
		val regionKey: ReferenceRegion = new ReferenceRegion(chr1, 0, 199)
		val region1: ReferenceRegion = new ReferenceRegion(chr1, 0L, 99L)
		val region2: ReferenceRegion = new ReferenceRegion(chr1, 100L, 199L)

		val read1 = (1L,2L)
		val read2 = (1L,500L)
		val read3 = (2L, 2L)
		val read4 =  (2L, 500L)

		var partition: IntervalPartition[Long, Long] = new IntervalPartition[Long, Long]()

		var newPartition = partition.multiput(region1, Iterator(read1, read3))
		newPartition = newPartition.multiput(region2, Iterator(read2, read4))

		// assert values are in the new partition
		var results: List[(Long, Long)] = newPartition.get(region1).toList
		results = results ++ newPartition.get(region2).toList

		assert(results.contains(read1))
		assert(results.contains(read2))
		assert(results.contains(read3))
		assert(results.contains(read4))

	}

	test("get some for iterator of intervals") {

		val chr1 = "chr1"
		val region1: ReferenceRegion = new ReferenceRegion(chr1, 0L, 99L)
		val region2: ReferenceRegion = new ReferenceRegion(chr1, 100L, 199L)

		val read1 = (1L,2L)
		val read2 = (1L,500L)
		val read3 = (2L, 6L)
		val read4 =  (2L, 500L)

		val iter = Iterator((region1, read1), (region2, read2), (region1, read3), (region2, read4))
		val partition = IntervalPartition(iter)

		var results = partition.get(region1, 1L).toList
		results = results ++ partition.get(region2, 2L).toList

		assert(results.contains(read1))
		assert(results.contains(read4))
	}

	test("putting differing number of reads into different regions") {
		val chr1 = "chr1"
		val regionKey: ReferenceRegion = new ReferenceRegion(chr1, 0, 199)
		val region1: ReferenceRegion = new ReferenceRegion(chr1, 0L, 99L)
		val region2: ReferenceRegion = new ReferenceRegion(chr1, 100L, 199L)

		val read1 = (1L,2L)
		val read2 = (1L,500L)
		val read3 = (2L, 2L)
		val read4 = (2L, 500L)
		val read5 = (3L, 500L)

		var partition: IntervalPartition[Long, Long] = new IntervalPartition[Long, Long]()
		val iter1 = Iterator(read1, read3)
		val iter2 = Iterator(read4, read2, read5)

		var newPartition = partition.multiput(region1, iter1)
		newPartition = newPartition.multiput(region2, iter2)

		// assert values are in the new partition
		var results: List[(Long, Long)] = newPartition.get(region1).toList
		results = results ++ newPartition.get(region2).toList

		assert(results.contains(read1))
		assert(results.contains(read2))
		assert(results.contains(read3))
		assert(results.contains(read4))
		assert(results.contains(read5))

	}


	test("putting then getting a region that overlaps 3 regions") {
		val chr1 = "chr1"
		val regionKey: ReferenceRegion = new ReferenceRegion(chr1, 0, 800)
		val region1: ReferenceRegion = new ReferenceRegion(chr1, 0L, 99L)
		val region2: ReferenceRegion = new ReferenceRegion(chr1, 100L, 199L)
		val region3: ReferenceRegion = new ReferenceRegion(chr1, 150L, 300L)
		val region4: ReferenceRegion = new ReferenceRegion(chr1, 350L, 700L)

		val read1 = (1L,2L)
		val read2 = (1L,500L)
		val read3 = (2L, 2L)
		val read4 = (2L, 500L)
		val read5 = (3L, 500L)
		val read6 = (4L, 250L)

		var partition: IntervalPartition[Long, Long] = new IntervalPartition[Long, Long]()

		var newPartition = partition.multiput(region1, Iterator(read1, read3))
		newPartition = newPartition.multiput(region2, Iterator(read2, read4))
		newPartition = newPartition.multiput(region3, Iterator(read5))
		newPartition = newPartition.multiput(region4, Iterator(read6))

		val overlapReg: ReferenceRegion = new ReferenceRegion(chr1, 0L, 200L)
		// assert values are in the new partition
		val results = newPartition.get(overlapReg).toList
	  assert(results.size == 5)
		println(results.size)

	}

}
