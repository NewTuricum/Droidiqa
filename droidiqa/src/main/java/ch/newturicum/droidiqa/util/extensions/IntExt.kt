package ch.newturicum.droidiqa.util.extensions

fun Int.pack(other: Int): Int {
    this.shr(16)
    if (this.shr(16) > 0 || other.shr(16) > 0) {
        return 0 // Both ints must be 16 bits length at most
    }
    return this.shl(16) + other
}