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
    const message = data?.message || data || `Request failed: ${response.status}`;
    throw new Error(message);
  }

  return data;
}

function App() {
  const [products, setProducts] = useState([]);
  const [cart, setCart] = useState(null);
  const [orders, setOrders] = useState([]);
  const [customerToken, setCustomerToken] = useState(localStorage.getItem("customerToken") || "");
  const [adminToken, setAdminToken] = useState(localStorage.getItem("adminToken") || "");
  const [guestCartId, setGuestCartId] = useState(localStorage.getItem("guestCartId") || "");
  const [message, setMessage] = useState("Ready");
  const [busy, setBusy] = useState(false);

  const [login, setLogin] = useState({ email: "customer@example.com", password: "password123" });
  const [adminLogin, setAdminLogin] = useState({ email: "admin@shop.com", password: "admin123" });
  const [register, setRegister] = useState({
    email: "",
    phone: "",
    username: "",
    password: "",
    confirmPassword: ""
  });
  const [otp, setOtp] = useState("");
  const [brand, setBrand] = useState({ name: "", logoUrl: "" });
  const [product, setProduct] = useState({
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
    const data = await api("/products");
    setProducts(data || []);
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
    const data = await api("/orders", {}, token);
    setOrders(data || []);
  }

  useEffect(() => {
    loadProducts().catch((error) => setMessage(error.message));
    loadCart("").catch((error) => setMessage(error.message));
  }, []);

  useEffect(() => {
    if (customerToken) {
      loadCart().catch((error) => setMessage(error.message));
      loadOrders().catch((error) => setMessage(error.message));
    }
  }, [customerToken]);

  async function loginCustomer() {
    await run("Customer login", async () => {
      const data = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify({ ...login, guestCartId })
      });
      setCustomerToken(data.token);
      localStorage.setItem("customerToken", data.token);
      await loadCart(data.token);
      await loadOrders(data.token);
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

      await loadCart();
      await loadOrders();
      await loadProducts();
    });
  }

  async function createBrand() {
    await run("Creating brand", async () => {
      const created = await api(
        "/admin/brand",
        {
          method: "POST",
          body: JSON.stringify(brand)
        },
        adminToken
      );
      setProduct((current) => ({ ...current, brandId: String(created.id) }));
      setBrand({ name: "", logoUrl: "" });
    });
  }

  async function createProduct() {
    await run("Creating product", async () => {
      await api(
        "/admin/product",
        {
          method: "POST",
          body: JSON.stringify({
            name: product.name,
            description: product.description,
            price: Number(product.price),
            quantity: Number(product.quantity),
            imageUrl: product.imageUrl,
            category: product.category,
            brand: { id: Number(product.brandId) }
          })
        },
        adminToken
      );
      setProduct({
        name: "",
        description: "",
        price: "",
        quantity: "",
        imageUrl: "",
        category: "ELECTRONICS",
        brandId: ""
      });
      await loadProducts();
    });
  }

  return (
    <main className="app">
      <header className="topbar">
        <div>
          <h1>E-Commerce Backend Console</h1>
          <p>{message}</p>
        </div>
        <button className="iconButton" onClick={() => run("Refreshing", async () => {
          await loadProducts();
          await loadCart();
          await loadOrders();
        })} disabled={busy} title="Refresh">
          <RefreshCcw size={18} />
        </button>
      </header>

      <section className="grid">
        <Panel title="Customer Access" icon={<LogIn size={18} />}>
          <div className="formGrid">
            <input value={login.email} onChange={(event) => setLogin({ ...login, email: event.target.value })} placeholder="Email" />
            <input value={login.password} onChange={(event) => setLogin({ ...login, password: event.target.value })} placeholder="Password" type="password" />
            <button onClick={loginCustomer}>Login</button>
          </div>
          <div className="statusLine">{customerToken ? "Customer token active" : "Login required for cart and checkout"}</div>
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

        <Panel title="Admin Catalog" icon={<Shield size={18} />}>
          <div className="formGrid">
            <input value={adminLogin.email} onChange={(event) => setAdminLogin({ ...adminLogin, email: event.target.value })} placeholder="Admin email" />
            <input value={adminLogin.password} onChange={(event) => setAdminLogin({ ...adminLogin, password: event.target.value })} placeholder="Admin password" type="password" />
            <button onClick={loginAdmin}>Admin Login</button>
          </div>
          <div className="splitForm">
            <div className="formGrid compact">
              <input value={brand.name} onChange={(event) => setBrand({ ...brand, name: event.target.value })} placeholder="Brand name" />
              <input value={brand.logoUrl} onChange={(event) => setBrand({ ...brand, logoUrl: event.target.value })} placeholder="Logo URL" />
              <button onClick={createBrand} disabled={!adminToken}>Create Brand</button>
            </div>
            <div className="formGrid compact">
              <input value={product.name} onChange={(event) => setProduct({ ...product, name: event.target.value })} placeholder="Product name" />
              <input value={product.description} onChange={(event) => setProduct({ ...product, description: event.target.value })} placeholder="Description" />
              <input value={product.price} onChange={(event) => setProduct({ ...product, price: event.target.value })} placeholder="Price" type="number" />
              <input value={product.quantity} onChange={(event) => setProduct({ ...product, quantity: event.target.value })} placeholder="Quantity" type="number" />
              <input value={product.imageUrl} onChange={(event) => setProduct({ ...product, imageUrl: event.target.value })} placeholder="Image URL" />
              <input value={product.brandId} onChange={(event) => setProduct({ ...product, brandId: event.target.value })} placeholder="Brand ID" type="number" />
              <select value={product.category} onChange={(event) => setProduct({ ...product, category: event.target.value })}>
                <option>ELECTRONICS</option>
                <option>CLOTHING</option>
                <option>FOOTWEAR</option>
                <option>ACCESSORIES</option>
                <option>JWELLERY</option>
              </select>
              <button onClick={createProduct} disabled={!adminToken}>Create Product</button>
            </div>
          </div>
        </Panel>

        <Panel title={`Products (${products.length})`} icon={<Boxes size={18} />}>
          <div className="products">
            {products.map((item) => (
              <article className="product" key={item.id}>
                <img src={item.imageUrl || "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600"} alt={item.name} />
                <div>
                  <h3>{item.name}</h3>
                  <p>{item.description}</p>
                  <div className="meta">
                    <span>{money(item.price)}</span>
                    <span>{item.quantity} in stock</span>
                  </div>
                </div>
                <button onClick={() => addToCart(item.id)} disabled={!customerToken || item.quantity < 1}>
                  <ShoppingCart size={16} /> Add
                </button>
              </article>
            ))}
          </div>
        </Panel>

        <Panel title={`Cart (${cartCount})`} icon={<ShoppingCart size={18} />}>
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
        </Panel>

        <Panel title="Checkout" icon={<CreditCard size={18} />}>
          <div className="formGrid compact">
            <input value={checkout.fullName} onChange={(event) => setCheckout({ ...checkout, fullName: event.target.value })} placeholder="Full name" />
            <input value={checkout.phone} onChange={(event) => setCheckout({ ...checkout, phone: event.target.value })} placeholder="Phone" />
            <input value={checkout.line1} onChange={(event) => setCheckout({ ...checkout, line1: event.target.value })} placeholder="Address line 1" />
            <input value={checkout.line2} onChange={(event) => setCheckout({ ...checkout, line2: event.target.value })} placeholder="Address line 2" />
            <input value={checkout.city} onChange={(event) => setCheckout({ ...checkout, city: event.target.value })} placeholder="City" />
            <input value={checkout.state} onChange={(event) => setCheckout({ ...checkout, state: event.target.value })} placeholder="State" />
            <input value={checkout.postalCode} onChange={(event) => setCheckout({ ...checkout, postalCode: event.target.value })} placeholder="Postal code" />
            <input value={checkout.country} onChange={(event) => setCheckout({ ...checkout, country: event.target.value })} placeholder="Country" />
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
                  <span>{order.status} · {money(order.total)}</span>
                </div>
                <span>{order.items.length} items</span>
              </div>
            ))}
          </div>
        </Panel>
      </section>
    </main>
  );
}

function Panel({ title, icon, children }) {
  return (
    <section className="panel">
      <div className="panelHeader">
        <span>{icon}</span>
        <h2>{title}</h2>
      </div>
      {children}
    </section>
  );
}

createRoot(document.getElementById("root")).render(<App />);
