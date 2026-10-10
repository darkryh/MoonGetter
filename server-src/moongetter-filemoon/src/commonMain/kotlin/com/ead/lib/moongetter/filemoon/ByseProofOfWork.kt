package com.ead.lib.moongetter.filemoon

import kotlinx.coroutines.yield

/** Work function used by the inspected Byse player bundle, despite the API's SHA-256 label. */
internal object ByseProofOfWork {
    // Bound computation and yield between batches so cancellation works on JVM and Native.
    suspend fun solve(nonce: String, difficulty: Int, maxAttempts: Int = 1_000_000): String? {
        require(nonce.length in 1..256 && nonce.all { it.code in 0..127 })
        require(difficulty in 0..24 && maxAttempts > 0)
        repeat(maxAttempts) { candidate ->
            if (candidate % 1024 == 0) yield()
            if (leadingZeros(hash("$nonce:$candidate")) >= difficulty) return candidate.toString()
        }
        return null
    }

    private fun rotate(value: Int, bits: Int) = (value shl bits) or (value ushr (32 - bits))

    private fun mix(state: IntArray) {
        state[0] += state[1]
        state[3] = rotate(state[3] xor state[0], 16)
        state[2] += state[3]
        state[1] = rotate(state[1] xor state[2], 12)
        state[0] += state[1]
        state[3] = rotate(state[3] xor state[0], 8)
        state[2] += state[3]
        state[1] = rotate(state[1] xor state[2], 7)
    }

    private fun hash(input: String): IntArray {
        val state = intArrayOf(1779033703, 3144134277L.toInt(), 1013904242, 2773480762L.toInt())
        for (char in input) {
            state[0] += char.code and 255
            state[0] = rotate(state[0], 7)
            mix(state)
        }
        repeat(8) { mix(state) }
        val memory = IntArray(512) {
            mix(state)
            state[0] xor state[2]
        }
        repeat(2) {
            for (index in memory.indices) {
                val address = memory[index] and 511
                var value = rotate(memory[index] + memory[address], 13)
                value = value xor (memory[(index + 1) and 511] * 2654435761L.toInt())
                memory[index] = value
                state[0] = state[0] xor value
                mix(state)
            }
        }
        return IntArray(8) { index ->
            mix(state)
            var value = state[0]
            repeat(64) { offset ->
                val item = memory[index * 64 + offset]
                value = rotate(value + item, 5) xor (item * 2246822519L.toInt())
            }
            value xor state[2]
        }
    }

    private fun leadingZeros(hash: IntArray): Int {
        var total = 0
        for (word in hash) {
            total += word.countLeadingZeroBits()
            if (word != 0) break
        }
        return total
    }
}
