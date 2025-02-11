[<img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg">](https://modrinth.com/lanitium)
[<img alt="github" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg">](https://github.com/iTutFadU/lanitium)

# Lanitium

**A Carpet extension with useful and advanced features**

Uses [Biscuit!](https://modrinth.com/mod/biscuit!)

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

### `lazy(expr?)`
Returns a lazy value of an expression in current `context`.
- `expr?` - The lazy expression. `null` if omitted

### `lazy(func) -> expr`
Defines function `func` as lazy. That is, all arguments are wrapped in `lazy` values.
- `func` - Function signature (i.e. `flat_map(list, expr)`)
- `expr` - Function body. Lazy values are returned lazily

### `lazy_call(lazy, vars, context?, type?)`
Evaluates (calls) a `lazy` value with additional variables and, optionally, custom `context` and context type. An additional variable `@` (`var('@')`) by default is set to the `lazy` value itself, enabling recursion and access to the original context even if a different one is used in the arguments.
- `lazy` - The lazy value to call. Use `lazy()` or call a lazy function to get one
- `vars` - A map of additional variables, from variable's name to its respective value
- `context?` - `context` to use when calling `lazy`. `lazy~'context'` if omitted
- `type?` - Context type to use when calling `lazy`. `lazy~'type'` if omitted

  If `lazy` calls `break()`, `continue()`, or `return()`, the respective error is thrown.

### `strict(value)`
Changes the strictness of current script host (like `'strict'` key in `__config()`).
- `value` - Strictness

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

## `context`
Stores local variables.

### `context:var`
Returns the lazy value stored in the variable.

### `context:var = lazy`
Assigns variable a lazy value to the variable.

### `has(context:var)`
Returns `true` if the variable is defined, `false` otherwise.

### `delete(context:var)`
Deletes the variable, returning `true` if it was defined before, and `false` otherwise.

### `length(context)`
Returns the amount of variables.

### `copy(context)`
Creates a duplicate context. **Does not copy over the variables.** Can be useful though.

### `context~'strict'`
Returns `true` if the script host is strict (`'strict'` is set to `true` in `__config()`), `false` otherwise.