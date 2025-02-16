[<img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg">](https://modrinth.com/lanitium)
[<img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg">](https://github.com/iTutFadU/lanitium)

# Lanitium

**A Carpet extension with useful and advanced features**

Uses [Biscuit!](https://modrinth.com/mod/biscuit!)

Better docs eventually...

# Config
The configuration file is stored at `config/lanitium.json`.
- `"mod_name"` - Custom mod name (the `"fabric" server` string in F3). If omitted, the default one is used (usually `fabric`). Only legacy formatting (using `§`) is supported.
- `"links"` - A list of server links. If omitted, the `bug-report` field in `server.properties` is used for a single link.
  - `"link"` - The link URL (actually, URI).
  - `"name"` - Formatted name of the link. If present, `"type"` is ignored.
  - `"type"` - A known link type. Can be one of:
    - `"report_bug"`
    - `"community_guidelines"`
    - `"support"`
    - `"status"`
    - `"feedback"`
    - `"community"`
    - `"website"`
    - `"forums"`
    - `"news"`
    - `"announcements"`
- `"display_motd"` - Default value for custom MOTD, formatted.
- `"display_players_online"` - Default value for custom player count.
- `"display_players_max"` - Default value for custom _displayed_ player limit.
- `"display_players_sample"` - Default value for custom player sample, a list of player names. Omit for default behavior.

# Functions

### `cookie(player, callback)`
Requests the cookie (`lanitium:cookie`) from the player. Returns a `lanitium_cookie_future`, see below.
- `player` - Player for which to get the cookie
- `callback(player, cookie)` - The function to call when the cookie is received
  
  If `'set'` is returned, the modified cookie is sent to the player.
  - `cookie` - The cookie. A map from `string` to `nbt`

### `cookie_reset(player)`
Resets the player's cookie to an empty map.

### `cookie_secret(secret)`
Sets the cookie secret. Used for validation, apparently.
- `secret` - The secret string

### `\(expr)`
Returns a lazy value of an expression in current `context`.
- `expr` - The lazy expression

### `\func -> expr`
Defines a lazy function. That is, all arguments are wrapped in lazy values.
- `func(...)` - Function signature
- `expr` - Function body

  **Does not work for function arguments in built-in functions.**

### `lazy_call(lazy, vars, context?, type?)`
Evaluates (calls) a lazy value with additional variables and, optionally, custom `context` and context type. An additional variable `@` (`var('@')`) by default is set to `lazy` itself, enabling recursion and access to the original context even if a different one is used in the arguments.
- `lazy` - The lazy value to call. Use `\(expr)` or call a lazy function to get one
- `vars` - A map of additional variables, from variable's name to its respective value
- `context?` - `context` to use when calling `lazy`. `lazy~'context'` if omitted
- `type?` - Context type to use when calling `lazy`. `lazy~'type'` if omitted

  If `lazy` calls `break()`, `continue()`, or `return()`, the respective error is thrown.

### `strict(value)`
Changes the strictness of current script host (like `'strict'` key in `__config()`).
- `value` - Strictness

### `iterator(has_next, next, reset, state)`
Creates a custom iterator.
- `has_next(state)` - Called before each iteration. If `false` is returned, the iteration is stopped
- `hext(state)` - Called each iteration. The returned value is used as the current value (`_`) in a loop
- `reset(state)` - Called to reset the iterator. Should reset the state of iteration
- `state` - The state to pass to the iteration functions. Cannot me reassigned, but can be modified. Is copied when copying the iterator

### `z(...exprs)`
Creates a list of lazy values.
- `...exprs` - The lazy values (expressions)

### `func\z(...args)`
Calls `func` with `...args`. Equivalent to `call(func, ...args)`.

### `all_then(...exprs)`
Similar to `then()` or `;`, but it always evaluates all expressions, throwing the last thrown error if there was any, and returning the value of the last expression if there wasn't.
- `...exprs` - Expressions separated by commas

### `catch_all(expr)`
Evaluates the expression, returning `null` if any error occurred.
- `expr` - Expression to evaluate

### `symbol()`
Creates a unique symbol, only equal to itself.

### `thread_local(initial, ...args)`
Creates a thread local value, local to each thread.
- `initial()` - Function to initialize the value
- `...args` - Additional arguments for `initial()`

### `system_info(info)`
- `'server_tps'` - Target server TPS. Isn't affected by freezing or sprinting
- `'server_frozen'` - `true` if the server is frozen (via `/tick freeze`), `false` otherwise
- `'server_sprinting'` - `true` if the server is sprinting (via `/tick sprint`), `false` otherwise

### `set_server_tps(tps)`
Sets the target server TPS. Works like `/tick rate <rate>`. Minimum TPS is 1.
- `tps` - The new tick rate

### `set_server_frozen(frozen)`
Freezes or unfreezes the server. Works like `/tick (freeze|unfreeze)`.
- `frozen` - Whether to freeze or unfreeze the server

### `server_sprint(ticks)`
Begins a server sprint. Works like `/tick sprint (<time>|stop)`.
- `ticks` - The amount of ticks to sprint. Use `0` to stop sprinting

### `display_server_motd(motd?)`
Sets or resets custom MOTD. If custom MOTD is reset, the one from `server.properties` is used.
- `motd?` - A format string to use as the new custom MOTD. Omit to reset custom MOTD

### `display_server_players_online(count?)`
Sets or resets custom player count in server list. If custom player count is reset, the actual player count is used.
- `count?` - Custom player count. Omit to use the actual player count

### `display_server_players_max(limit?)`
Sets or resets custom _displayed_ player limit in server list. If custom _displayed_ player limit is reset, the one from `server.properties` is used.
- `limit?` - Custom _displayed_ player limit. Omit to use the actual player limit

### `display_server_players_sample(...names)`
Sets custom player sample in server list (when hovering over player count).
- `...names` - Names of players to display in the sample.

### `display_server_players_sample_default()`
Resets custom player sample to default behavior.

### `format_json(json)`
Returns a `text` (like `format()`) using vanilla text component deserialization (like `/tellraw`).
- `json` - The JSON component to deserialize

  Throws `json_error` on a syntax or schema error.

  **Note:** You can use `encode_nbt(text, true):''` to serialize a `text` (get raw JSON). [Blame Gnembon.](https://github.com/gnembon/fabric-carpet/commit/4e57eecf2d29718cd03b77eb4167c1829e6e2dd7?w=0#diff-5a29389bd119769dea9834d3916453aa270116b70b011cb46aeaf44457121e80L93)

### `as_entity(entity, expr)`
Changes source entity for the expression inside. Works like `in_dimension()` or `/execute as <entity>` (with one entity).
- `entity` - New source entity
- `expr` - Expression with a modified context

### `positioned(pos, expr)`
Changes source position for the expression inside. Works like `in_dimension()` or `/execute positioned <pos>`.
- `pos` - New source position
- `expr` - Expression with a modified context

### `rotated(rot, expr)`
Changes source rotation for the expression inside. Works like `in_dimension()` or `/execute positioned <rot>`. Format is `[pitch, yaw]`.
- `rot` - New source rotation
- `expr` - Expression with a modified context

### `send_success(message, broadcast?)`
Sends a success message (like 'Filled 121 blocks').
- `message` - Formatted message
- `broadcast?` - If `true`, the message will be broadcast to admins (like '_[Player: Filled 121 blocks]_')

### `send_failure(message)`
Sends a red failure message (like 'That position is out of this world!').
- `message` - Formatted message, colored red by default

### `send_system_message(message)`
Sends a system message (like 'Player joined the game').
- `message` - Formatted message

# Types

## `lanitium_cookie_future`
Stores the state of a cookie operation.

### `future~'done'`
Returns whether the future is done. `true` if the callback finished or the future has been cancelled (if there's no cookie), `false` otherwise.

### `future~'cancelled'`
Returns whether the future has been cancelled. `true` if there was an error (i.e. there's no cookie), `false` otherwise.

## `lazy`
A lazy value, storing a code snippet (expression), `context`, and context type.

### `lazy~'context'`
Returns the `context` of this lazy value.

### `lazy~'type'`
Returns the context type of this `lazy` value. Possible values:
- `'none'`
- `'void'`
- `'boolean'`
- `'number'`
- `'string'`
- `'list'`
- `'iterator'`
- `'signature'`
- `'localization'`
- `'lvalue'`
- `'mapdef'`

### `lazy\(expr)`
Executes `expr` in the context of this lazy.

## `context`
Stores local variables and script host.

### `copy(context)`
Creates a duplicate context. **Does not copy over the variables.** Can be useful though.

### `context~'strict'`
Returns `true` if the script host is strict (`'strict'` is set to `true` in `__config()`), `false` otherwise.

### `context\(expr)`
Executes `expr` in this context.

## `symbol`
A unique value, only equal to itself.

## `thread_local`
A thread local value, local to each thread. Serialization is delegated.

### `thread_local:null`
Gets the value. If it's _removed_, it is first initialized via the `initial()` function.

### `thread_local:null = value`
Sets the value.

### `delete(thread_local:null)`
Removes the value. If the value isn't set before getting it, it is _removed_ by default.

# Miscellaneous

Event `player_command(player, command)` is now cancellable, except when the command begins with `"script "`. Be careful with it.