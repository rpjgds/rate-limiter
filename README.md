## Sliding Window Rate Limiter

PoC implementation of [this algorithm](https://blog.cloudflare.com/counting-things-a-lot-of-different-things/#sliding-windows-to-the-rescue).

`check` method follows the implementation reasonably closely. `checkAndRecord` is a non-threadsafe way to update the local data to allow esier testing (`check_cumulative`).


