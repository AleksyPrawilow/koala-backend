# koala-backend

## Authentication

`JWT_SECRET_BASE64` is required; the application fails at startup if it is missing or shorter than 32 decoded bytes. Generate a fresh key for each environment and store it in your deployment's secret manager:

```sh
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
```

Do not use a fixed fallback such as `1234`: anyone who knows it could create tokens, including tokens claiming the `ADMIN` role. The key is Base64-encoded in the environment, not committed to application configuration. `JWT_EXPIRATION` defaults to `15m`. The auth cookie is `Secure` by default; set `JWT_COOKIE_SECURE=false` only for local HTTP development. `JWT_COOKIE_SAME_SITE` defaults to `Lax`; use `None` for cross-site browser clients, which requires HTTPS and a secure cookie.

### Try the flow in Postman

The Compose file starts PostgreSQL with `docker compose up`; the application runs locally with Gradle. From the repository root, start the database and app:

```sh
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
export JWT_COOKIE_SECURE=false
docker compose up -d
./gradlew bootRun
```

In Postman, create an environment with `baseUrl` set to `http://localhost:8080`, and use the same host for every request so Postman keeps the cookies together. Leave the Authorization tab set to **No Auth**; the backend reads JWTs from cookies, not bearer headers.

1. **Get a CSRF token:** Send `GET {{baseUrl}}/api/auth/csrf`. The response body contains `{"token":"..."}` and sets an `XSRF-TOKEN` cookie. In the request's **Scripts → Post-response** tab, save the token:

   ```js
   pm.environment.set("csrfToken", pm.response.json().token);
   ```

   Postman should retain the CSRF cookie in its cookie jar.

2. **Register a test user** (skip this if you already have credentials): Send `POST {{baseUrl}}/api/auth/register` with `Content-Type: application/json`, `X-XSRF-TOKEN: {{csrfToken}}`, and this body:

   ```json
   {
     "email": "postman-test@example.com",
     "password": "Password1!",
     "firstName": "Postman",
     "lastName": "Tester",
     "role": { "id": 2, "name": "USER" }
   }
   ```

   This registers a regular user, not an administrator. On a fresh database, the migrations assign `USER` role ID `2`; check the `roles` table if your database differs. For the default local database, you can run `docker compose exec postgres psql -U koala_user -d koala -c 'SELECT id, name FROM roles ORDER BY id;'`.

3. **Log in:** First repeat the CSRF request (or reuse the current token and cookie). Then send `POST {{baseUrl}}/api/auth/login` with `Content-Type: application/json`, `X-XSRF-TOKEN: {{csrfToken}}`, and:

   ```json
   {
     "email": "postman-test@example.com",
     "password": "Password1!"
   }
   ```

   A successful response returns the user's basic profile and sets the `AUTH_TOKEN` cookie. Postman should store and send it automatically. Invalid credentials return `401`.


4. **Verify role authorization:** Send `GET {{baseUrl}}/api/admin/users` as the test user; it should return `403`. Registration cannot create an `ADMIN`, so a successful admin request requires an already provisioned active administrator.

For any later state-changing request (`POST`, `PATCH`, `PUT`, or `DELETE`), send the CSRF header and retain the `XSRF-TOKEN` cookie. If a permitted auth request unexpectedly returns `401`, clear any old `AUTH_TOKEN` cookie in Postman's cookie manager and retry. A missing or mismatched CSRF token normally returns `403`.

### Browser login flow

The JWT is in an `HttpOnly` cookie, so frontend JavaScript must not try to read or store it. The browser sends it automatically when requests include credentials.

1. Request `GET /api/auth/csrf`; keep the returned `token` for the next request. The response also sets the CSRF cookie.
2. Send `POST /api/auth/login` with `{"email":"...","password":"..."}`, the `X-XSRF-TOKEN` header, and credentials enabled. A successful response returns the user's basic profile and sets the `AUTH_TOKEN` cookie. Invalid credentials return `401`.
3. Include credentials on subsequent API requests. Add the CSRF token header to every state-changing request (`POST`, `PATCH`, `PUT`, or `DELETE`), including registration.
4. A missing or expired JWT returns `401`; an authenticated non-admin calling `/api/admin/**` gets `403`. Tokens expire after 15 minutes by default, after which the user must log in again.

Example with `fetch`:

```js
const api = "https://api.example.com";
const csrfResponse = await fetch(`${api}/api/auth/csrf`, {
  credentials: "include",
});
const { token } = await csrfResponse.json();

const loginResponse = await fetch(`${api}/api/auth/login`, {
  method: "POST",
  credentials: "include",
  headers: {
    "Content-Type": "application/json",
    "X-XSRF-TOKEN": token,
  },
  body: JSON.stringify({ email, password }),
});
```

For local development, credentialed CORS allows `http://localhost` and `https://localhost` on any port. To override this in another environment, set `CORS_ALLOWED_ORIGIN_PATTERNS` to a comma-separated list of trusted origin patterns. Credentials are enabled, so do not use `*`; set only origins you control. For a cross-site frontend, also configure `JWT_COOKIE_SAME_SITE=None` and HTTPS (`JWT_COOKIE_SECURE=true`).

The backend currently has no logout or “current user” endpoint. Add these if the UI needs to clear the cookie immediately or restore the profile after a page reload; without a logout endpoint, a JWT remains usable until it expires.
