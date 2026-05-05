import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  BadgeIndianRupee,
  Boxes,
  CreditCard,
  LogIn,
  PackagePlus,
  RefreshCcw,
  Shield,
  ShoppingCart,
  Trash2,
  UserPlus
} from "lucide-react";
import "./styles.css";

const API_BASE = "http://localhost:8080";

const money = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
  }).format(Number(value || 0));

async function api(path, options = {}, token) {
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  });

  const text = await response.text();
  let data = text;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    throw new Error(data?.message || data || `Request failed: ${response.status}`);
  }
  return data;
}

function App() {
  const [view, setView] = useState("store");
  const [products, setProducts] = useState([]);
  const [adminProducts, setAdminProducts] = useState([]);
  const [brands, setBrands] = useState([]);
  const [cart, setCart] = useState(null);
  const [orders, setOrders] = useState([]);
  const [customerToken, setCustomerToken] = useState(localStorage.getItem("customerToken") || "");
  const [adminToken, setAdminToken] = useState(localStorage.getItem("adminToken") || "");
  const [guestCartId, setGuestCartId] = useState(localStorage.getItem("guestCartId") || "");
  const [message, setMessage] = useState("Ready");
  const [busy, setBusy] = useState(false);

  const [login, setLogin] = useState({ email: "customer@example.com", password: "password123" });
  const [googleLogin, setGoogleLogin] = useState({ email: "", name: "", idToken: "" });
  const [adminLogin, setAdminLogin] = useState({ email: "admin@shop.com", password: "admin123" });
  const [register, setRegister] = useState({
    email: "",
    phone: "",
    username: "",
    password: "",
    confirmPassword: ""
  });
  const [otp, setOtp] = useState("");
  const [brand, setBrand] = useState({ id: "", name: "", logoUrl: "" });
  const [product, setProduct] = useState({
    id: "",
    name: "",
    description: "",
    price: "",
    quantity: "",
    imageUrl: "",
    category: "ELECTRONICS",
    brandId: ""
  });
  const [checkout, setCheckout] = useState({
    fullName: "Customer",
    phone: "9999999999",
    line1: "221B Baker Street",
    line2: "Floor 2",
    city: "Bengaluru",
    state: "Karnataka",
    postalCode: "560001",
    country: "India",
    paymentMethod: "CARD"
  });

  const cartCount = useMemo(
    () => cart?.items?.reduce((sum, item) => sum + item.quantity, 0) || 0,
    [cart]
  );

  async function run(label, task) {
    setBusy(true);
    setMessage(label);
    try {
      const result = await task();
      setMessage(`${label} complete`);
      return result;
    } catch (error) {
      setMessage(error.message);
      throw error;
    } finally {
      setBusy(false);
    }
  }

  async function loadProducts() {
    setProducts((await api("/products")) || []);
  }

  async function loadCart(token = customerToken) {
    const data = await api("/cart", {
      headers: guestCartId && !token ? { "X-Guest-Cart-Id": guestCartId } : {}
    }, token);
    if (!token && data?.cartKey && !data.cartKey.startsWith("user:")) {
      setGuestCartId(data.cartKey);
      localStorage.setItem("guestCartId", data.cartKey);
    }
    setCart(data);
  }

  async function loadOrders(token = customerToken) {
    if (!token) return;
    setOrders((await api("/orders", {}, token)) || []);
  }

  async function loadAdminData(token = adminToken) {
    if (!token) return;
    const [brandData, productData] = await Promise.all([
      api("/admin/brands", {}, token),
      api("/admin/products", {}, token)
    ]);
    setBrands(brandData || []);
    setAdminProducts(productData || []);
  }

  useEffect(() => {
    loadProducts().catch((error) => setMessage(error.message));
    loadCart("").catch((error) => setMessage(error.message));
  }, []);

  useEffect(() => {
    if (customerToken) {
      loadCart(customerToken).catch((error) => setMessage(error.message));
      loadOrders(customerToken).catch((error) => setMessage(error.message));
    }
  }, [customerToken]);

  useEffect(() => {
    if (adminToken) {
      loadAdminData(adminToken).catch((error) => setMessage(error.message));
    }
  }, [adminToken]);

  async function loginCustomer() {
    await run("Customer login", async () => {
      const data = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify({ ...login, guestCartId })
      });
      setCustomerToken(data.token);
      localStorage.setItem("customerToken", data.token);
    });
  }

  async function loginWithGoogle() {
    await run("Google login", async () => {
      const data = await api("/auth/google", {
        method: "POST",
        body: JSON.stringify({ ...googleLogin, guestCartId })
      });
      setCustomerToken(data.token);
      localStorage.setItem("customerToken", data.token);
    });
  }

  async function loginAdmin() {
    await run("Admin login", async () => {
      const data = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify(adminLogin)
      });
      setAdminToken(data.token);
      localStorage.setItem("adminToken", data.token);
      await loadAdminData(data.token);
    });
  }

  async function registerRequest() {
    await run("Sending OTP", async () => {
      await api("/auth/register-request", {
        method: "POST",
        body: JSON.stringify(register)
      });
    });
  }

  async function verifyOtp() {
    await run("Verifying OTP", async () => {
      await api("/auth/register-verify", {
        method: "POST",
        body: JSON.stringify({ email: register.email, otp })
      });
      setLogin({ email: register.email, password: register.password });
      setView("account");
    });
  }

  async function addToCart(productId) {
    await run("Adding to cart", async () => {
      await api(
        "/cart/items",
        {
          method: "POST",
          headers: guestCartId && !customerToken ? { "X-Guest-Cart-Id": guestCartId } : {},
          body: JSON.stringify({ productId, quantity: 1 })
        },
        customerToken
      );
      await loadCart();
    });
  }

  async function updateCart(productId, quantity) {
    await run("Updating cart", async () => {
      const guestHeaders = guestCartId && !customerToken ? { "X-Guest-Cart-Id": guestCartId } : {};
      if (quantity < 1) {
        await api(`/cart/items/${productId}`, { method: "DELETE", headers: guestHeaders }, customerToken);
      } else {
        await api(`/cart/items/${productId}?quantity=${quantity}`, { method: "PUT", headers: guestHeaders }, customerToken);
      }
      await loadCart();
    });
  }

  async function placeOrder() {
    await run("Checkout", async () => {
      const order = await api(
        "/orders/checkout",
        {
          method: "POST",
          body: JSON.stringify({
            paymentMethod: checkout.paymentMethod,
            address: {
              fullName: checkout.fullName,
              phone: checkout.phone,
              line1: checkout.line1,
              line2: checkout.line2,
              city: checkout.city,
              state: checkout.state,
              postalCode: checkout.postalCode,
              country: checkout.country
            }
          })
        },
        customerToken
      );

      if (checkout.paymentMethod !== "CASH_ON_DELIVERY") {
        await api(
          `/payments/orders/${order.id}/pay`,
          {
            method: "POST",
            body: JSON.stringify({ providerToken: "frontend-test-token" })
          },
          customerToken
        );
      }

      await Promise.all([loadCart(), loadOrders(), loadProducts()]);
    });
  }

  async function saveBrand() {
    await run(brand.id ? "Updating brand" : "Creating brand", async () => {
      const created = await api(
        brand.id ? `/admin/brand/${brand.id}` : "/admin/brand",
        {
          method: brand.id ? "PUT" : "POST",
          body: JSON.stringify({ name: brand.name, logoUrl: brand.logoUrl })
        },
        adminToken
      );
      setProduct((current) => ({ ...current, brandId: String(created.id) }));
      setBrand({ id: "", name: "", logoUrl: "" });
      await loadAdminData();
    });
  }

  async function deleteBrand(id) {
    await run("Deleting brand", async () => {
      await api(`/admin/brand/${id}`, { method: "DELETE" }, adminToken);
      await loadAdminData();
    });
  }

  async function saveProduct() {
    await run(product.id ? "Updating product" : "Creating product", async () => {
      await api(
        product.id ? `/admin/product/${product.id}` : "/admin/product",
        {
          method: product.id ? "PUT" : "POST",
          body: JSON.stringify({
            name: product.name,
            description: product.description,
            price: Number(product.price),
            quantity: Number(product.quantity),
            imageUrl: product.imageUrl,
            category: product.category,
            brand: product.brandId ? { id: Number(product.brandId) } : null
          })
        },
        adminToken
      );
      resetProductForm();
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  async function updateInventory(item, quantity) {
    await run("Updating inventory", async () => {
      await api(`/admin/product/${item.id}/inventory?quantity=${Number(quantity)}`, { method: "PUT" }, adminToken);
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  async function deleteProduct(id) {
    await run("Deleting product", async () => {
      await api(`/admin/product/${id}`, { method: "DELETE" }, adminToken);
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  function editProduct(item) {
    setProduct({
      id: String(item.id),
      name: item.name || "",
      description: item.description || "",
      price: String(item.price || ""),
      quantity: String(item.quantity || ""),
      imageUrl: item.imageUrl || "",
      category: item.category || "ELECTRONICS",
      brandId: item.brand?.id ? String(item.brand.id) : ""
    });
  }

  function resetProductForm() {
    setProduct({
      id: "",
      name: "",
      description: "",
      price: "",
      quantity: "",
      imageUrl: "",
      category: "ELECTRONICS",
      brandId: ""
    });
  }

  return (
    <main className="app">
      <header className="topbar">
        <div>
          <h1>Shop Console</h1>
          <p>{message}</p>
        </div>
        <button className="iconButton" onClick={() => run("Refreshing", async () => {
          await Promise.all([loadProducts(), loadCart(), loadOrders(), loadAdminData()]);
        })} disabled={busy} title="Refresh">
          <RefreshCcw size={18} />
        </button>
      </header>

      <nav className="tabs">
        {[
          ["store", "Store"],
          ["account", "Account"],
          ["checkout", `Cart (${cartCount})`],
          ["admin", "Admin"]
        ].map(([key, label]) => (
          <button className={view === key ? "active" : ""} key={key} onClick={() => setView(key)}>
            {label}
          </button>
        ))}
      </nav>

      {view === "store" && (
        <section className="grid">
          <Panel title={`Products (${products.length})`} icon={<Boxes size={18} />} wide>
            <div className="products">
              {products.map((item) => (
                <article className="product" key={item.id}>
                  <img src={item.imageUrl} alt={item.name} />
                  <div>
                    <h3>{item.name}</h3>
                    <p>{item.description}</p>
                    <div className="meta">
                      <span>{money(item.price)}</span>
                      <span>{item.quantity} in stock</span>
                    </div>
                  </div>
                  <button onClick={() => addToCart(item.id)} disabled={item.quantity < 1}>
                    <ShoppingCart size={16} /> Add
                  </button>
                </article>
              ))}
            </div>
          </Panel>
        </section>
      )}

      {view === "account" && (
        <section className="grid">
          <Panel title="Customer Login" icon={<LogIn size={18} />}>
            <div className="formGrid">
              <input value={login.email} onChange={(event) => setLogin({ ...login, email: event.target.value })} placeholder="Email" />
              <input value={login.password} onChange={(event) => setLogin({ ...login, password: event.target.value })} placeholder="Password" type="password" />
              <button onClick={loginCustomer}>Login</button>
            </div>
            <div className="statusLine">{customerToken ? "Customer token active" : "Guest cart is active"}</div>
          </Panel>

          <Panel title="Google OAuth" icon={<LogIn size={18} />}>
            <div className="formGrid">
              <input value={googleLogin.idToken} onChange={(event) => setGoogleLogin({ ...googleLogin, idToken: event.target.value })} placeholder="Google ID token" />
              <input value={googleLogin.email} onChange={(event) => setGoogleLogin({ ...googleLogin, email: event.target.value })} placeholder="Local fallback email" />
              <input value={googleLogin.name} onChange={(event) => setGoogleLogin({ ...googleLogin, name: event.target.value })} placeholder="Display name" />
              <button onClick={loginWithGoogle}>Continue with Google</button>
            </div>
          </Panel>

          <Panel title="OTP Registration" icon={<UserPlus size={18} />}>
            <div className="formGrid compact">
              <input value={register.email} onChange={(event) => setRegister({ ...register, email: event.target.value })} placeholder="Email" />
              <input value={register.phone} onChange={(event) => setRegister({ ...register, phone: event.target.value })} placeholder="Phone" />
              <input value={register.username} onChange={(event) => setRegister({ ...register, username: event.target.value })} placeholder="Username" />
              <input value={register.password} onChange={(event) => setRegister({ ...register, password: event.target.value })} placeholder="Password" type="password" />
              <input value={register.confirmPassword} onChange={(event) => setRegister({ ...register, confirmPassword: event.target.value })} placeholder="Confirm password" type="password" />
              <button onClick={registerRequest}>Send OTP</button>
              <input value={otp} onChange={(event) => setOtp(event.target.value)} placeholder="OTP from backend console" />
              <button onClick={verifyOtp}>Verify</button>
            </div>
          </Panel>
        </section>
      )}

      {view === "checkout" && (
        <section className="grid">
          <Panel title={`Cart (${cartCount})`} icon={<ShoppingCart size={18} />}>
            <CartList cart={cart} updateCart={updateCart} />
          </Panel>

          <Panel title="Checkout" icon={<CreditCard size={18} />}>
            <div className="formGrid compact">
              {["fullName", "phone", "line1", "line2", "city", "state", "postalCode", "country"].map((field) => (
                <input key={field} value={checkout[field]} onChange={(event) => setCheckout({ ...checkout, [field]: event.target.value })} placeholder={field} />
              ))}
              <select value={checkout.paymentMethod} onChange={(event) => setCheckout({ ...checkout, paymentMethod: event.target.value })}>
                <option>CARD</option>
                <option>UPI</option>
                <option>NET_BANKING</option>
                <option>CASH_ON_DELIVERY</option>
              </select>
              <button onClick={placeOrder} disabled={!customerToken || !cart?.items?.length}>
                <BadgeIndianRupee size={16} /> Place Order
              </button>
            </div>
          </Panel>

          <Panel title={`Orders (${orders.length})`} icon={<PackagePlus size={18} />}>
            <div className="list">
              {orders.map((order) => (
                <div className="row" key={order.id}>
                  <div>
                    <strong>Order #{order.id}</strong>
                    <span>{order.status} - {money(order.total)}</span>
                  </div>
                  <span>{order.items.length} items</span>
                </div>
              ))}
            </div>
          </Panel>
        </section>
      )}

      {view === "admin" && (
        <section className="grid">
          <Panel title="Admin Login" icon={<Shield size={18} />}>
            <div className="formGrid">
              <input value={adminLogin.email} onChange={(event) => setAdminLogin({ ...adminLogin, email: event.target.value })} placeholder="Admin email" />
              <input value={adminLogin.password} onChange={(event) => setAdminLogin({ ...adminLogin, password: event.target.value })} placeholder="Admin password" type="password" />
              <button onClick={loginAdmin}>Admin Login</button>
            </div>
            <div className="statusLine">{adminToken ? "Admin token active" : "Admin credentials required"}</div>
          </Panel>

          <Panel title="Manage Brands" icon={<Shield size={18} />}>
            <div className="formGrid">
              <input value={brand.name} onChange={(event) => setBrand({ ...brand, name: event.target.value })} placeholder="Brand name" />
              <input value={brand.logoUrl} onChange={(event) => setBrand({ ...brand, logoUrl: event.target.value })} placeholder="Logo URL" />
              <button onClick={saveBrand} disabled={!adminToken}>{brand.id ? "Update Brand" : "Create Brand"}</button>
            </div>
            <div className="list spaced">
              {brands.map((item) => (
                <div className="row" key={item.id}>
                  <div>
                    <strong>{item.name}</strong>
                    <span>#{item.id}</span>
                  </div>
                  <div className="actions">
                    <button onClick={() => setBrand({ id: String(item.id), name: item.name || "", logoUrl: item.logoUrl || "" })}>Edit</button>
                    <button className="danger" onClick={() => deleteBrand(item.id)}><Trash2 size={14} /></button>
                  </div>
                </div>
              ))}
            </div>
          </Panel>

          <Panel title="Manage Products" icon={<PackagePlus size={18} />} wide>
            <div className="formGrid compact">
              <input value={product.name} onChange={(event) => setProduct({ ...product, name: event.target.value })} placeholder="Product name" />
              <input value={product.description} onChange={(event) => setProduct({ ...product, description: event.target.value })} placeholder="Description" />
              <input value={product.price} onChange={(event) => setProduct({ ...product, price: event.target.value })} placeholder="Price" type="number" />
              <input value={product.quantity} onChange={(event) => setProduct({ ...product, quantity: event.target.value })} placeholder="Quantity" type="number" />
              <input value={product.imageUrl} onChange={(event) => setProduct({ ...product, imageUrl: event.target.value })} placeholder="Image URL" />
              <select value={product.brandId} onChange={(event) => setProduct({ ...product, brandId: event.target.value })}>
                <option value="">No brand</option>
                {brands.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
              </select>
              <select value={product.category} onChange={(event) => setProduct({ ...product, category: event.target.value })}>
                <option>ELECTRONICS</option>
                <option>CLOTHING</option>
                <option>FOOTWEAR</option>
                <option>ACCESSORIES</option>
                <option>JWELLERY</option>
              </select>
              <button onClick={saveProduct} disabled={!adminToken}>{product.id ? "Update Product" : "Create Product"}</button>
            </div>
            <div className="list spaced">
              {adminProducts.map((item) => (
                <div className="row adminRow" key={item.id}>
                  <div>
                    <strong>{item.name}</strong>
                    <span>{money(item.price)} - {item.brand?.name || "No brand"}</span>
                  </div>
                  <input defaultValue={item.quantity} type="number" min="0" onBlur={(event) => updateInventory(item, event.target.value)} />
                  <div className="actions">
                    <button onClick={() => editProduct(item)}>Edit</button>
                    <button className="danger" onClick={() => deleteProduct(item.id)}><Trash2 size={14} /></button>
                  </div>
                </div>
              ))}
            </div>
          </Panel>
        </section>
      )}
    </main>
  );
}

function CartList({ cart, updateCart }) {
  return (
    <div className="list">
      {(cart?.items || []).map((item) => (
        <div className="row" key={item.productId}>
          <div>
            <strong>{item.productName}</strong>
            <span>{money(item.lineTotal)}</span>
          </div>
          <div className="stepper">
            <button onClick={() => updateCart(item.productId, item.quantity - 1)}>-</button>
            <span>{item.quantity}</span>
            <button onClick={() => updateCart(item.productId, item.quantity + 1)}>+</button>
          </div>
        </div>
      ))}
      <div className="total">
        <span>Subtotal</span>
        <strong>{money(cart?.subtotal || 0)}</strong>
      </div>
    </div>
  );
}

function Panel({ title, icon, children, wide = false }) {
  return (
    <section className={`panel ${wide ? "wide" : ""}`}>
      <div className="panelHeader">
        <span>{icon}</span>
        <h2>{title}</h2>
      </div>
      {children}
    </section>
  );
}

createRoot(document.getElementById("root")).render(<App />);
