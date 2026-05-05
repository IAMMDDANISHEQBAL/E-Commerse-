import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  BadgeIndianRupee,
  Boxes,
  CheckCircle2,
  CreditCard,
  Edit3,
  Grid2X2,
  Heart,
  LogIn,
  LogOut,
  Minus,
  PackageCheck,
  PackagePlus,
  Plus,
  RefreshCcw,
  Search,
  Shield,
  ShoppingCart,
  Trash2,
  UserPlus,
  X
} from "lucide-react";
import "./styles.css";

const API_BASE = "http://localhost:8080";
const categories = ["ALL", "ELECTRONICS", "CLOTHING", "FOOTWEAR", "ACCESSORIES", "JWELLERY"];

const emptyProduct = {
  id: "",
  name: "",
  description: "",
  price: "",
  quantity: "",
  imageUrl: "",
  category: "ELECTRONICS",
  brandId: ""
};

const defaultAddress = {
  fullName: "",
  phone: "",
  line1: "",
  line2: "",
  city: "",
  state: "",
  postalCode: "",
  country: "India",
  paymentMethod: "CARD"
};

const money = (value) =>
  new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(Number(value || 0));

function readStored(key) {
  return localStorage.getItem(key) || "";
}

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
    const validation = data?.validationErrors ? Object.values(data.validationErrors).join(", ") : "";
    throw new Error(validation || data?.message || data || `Request failed: ${response.status}`);
  }
  return data;
}

function loadScript(src) {
  return new Promise((resolve, reject) => {
    if (document.querySelector(`script[src="${src}"]`)) {
      resolve();
      return;
    }
    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.onload = resolve;
    script.onerror = () => reject(new Error(`Unable to load ${src}`));
    document.body.appendChild(script);
  });
}

function App() {
  const [page, setPage] = useState("home");
  const [products, setProducts] = useState([]);
  const [frontendConfig, setFrontendConfig] = useState({ paymentProvider: "mock", razorpayKeyId: "", googleClientId: "" });
  const [brands, setBrands] = useState([]);
  const [adminProducts, setAdminProducts] = useState([]);
  const [cart, setCart] = useState(null);
  const [orders, setOrders] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [customerToken, setCustomerToken] = useState(readStored("customerToken"));
  const [adminToken, setAdminToken] = useState(readStored("adminToken"));
  const [guestCartId, setGuestCartId] = useState(readStored("guestCartId"));
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("ALL");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState({});

  const [login, setLogin] = useState({ email: "", password: "" });
  const [googleLogin, setGoogleLogin] = useState({ email: "", name: "", idToken: "" });
  const [adminLogin, setAdminLogin] = useState({ email: "admin@shop.com", password: "admin123" });
  const [register, setRegister] = useState({ email: "", phone: "", username: "", password: "", confirmPassword: "" });
  const [otp, setOtp] = useState("");
  const [brandForm, setBrandForm] = useState({ id: "", name: "", logoUrl: "" });
  const [productForm, setProductForm] = useState(emptyProduct);
  const [checkout, setCheckout] = useState(defaultAddress);
  const [payment, setPayment] = useState({ orderId: "", providerOrderId: "", razorpayPaymentId: "", razorpaySignature: "" });
  const [paymentModal, setPaymentModal] = useState(null);

  const cartCount = useMemo(() => cart?.items?.reduce((sum, item) => sum + item.quantity, 0) || 0, [cart]);
  const cartTotal = Number(cart?.subtotal || 0);
  const shippingFee = cartTotal > 0 ? 49 : 0;

  const filteredProducts = useMemo(() => {
    const term = query.trim().toLowerCase();
    return products.filter((product) => {
      const matchesCategory = category === "ALL" || product.category === category;
      const matchesQuery = !term ||
        product.name?.toLowerCase().includes(term) ||
        product.description?.toLowerCase().includes(term) ||
        product.brand?.name?.toLowerCase().includes(term);
      return matchesCategory && matchesQuery;
    });
  }, [products, query, category]);

  useEffect(() => {
    Promise.all([loadConfig(), loadProducts(), loadCart("")]).catch(showError);
  }, []);

  useEffect(() => {
    if (customerToken) {
      Promise.all([loadCart(customerToken), loadOrders(customerToken)]).catch(showError);
    }
  }, [customerToken]);

  useEffect(() => {
    if (adminToken) {
      loadAdminData(adminToken).catch(showError);
    }
  }, [adminToken]);

  function showError(error) {
    setMessage(error.message);
  }

  async function run(label, task) {
    setBusy(true);
    setErrors({});
    setMessage(label);
    try {
      const result = await task();
      setMessage(`${label} complete`);
      return result;
    } catch (error) {
      showError(error);
      throw error;
    } finally {
      setBusy(false);
    }
  }

  async function loadProducts() {
    setProducts((await api("/products")) || []);
  }

  async function loadConfig() {
    setFrontendConfig(await api("/public/config"));
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

  function validate(fields) {
    const next = {};
    Object.entries(fields).forEach(([key, value]) => {
      if (value === null || value === undefined || String(value).trim() === "") {
        next[key] = "Required";
      }
    });
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function loginCustomer() {
    if (!validate({ email: login.email, password: login.password })) return;
    await run("Signing in", async () => {
      const data = await api("/auth/login", {
        method: "POST",
        body: JSON.stringify({ ...login, guestCartId })
      });
      setCustomerToken(data.token);
      localStorage.setItem("customerToken", data.token);
      setPage("cart");
      await Promise.all([loadCart(data.token), loadOrders(data.token)]);
    });
  }

  async function loginWithGoogle(idTokenOverride = "") {
    const payload = idTokenOverride ? { ...googleLogin, idToken: idTokenOverride } : googleLogin;
    if (!payload.idToken && !validate({ email: payload.email })) return;
    await run("Google sign in", async () => {
      const data = await api("/auth/google", {
        method: "POST",
        body: JSON.stringify({ ...payload, guestCartId })
      });
      setCustomerToken(data.token);
      localStorage.setItem("customerToken", data.token);
      setPage("cart");
    });
  }

  async function registerRequest() {
    if (!validate({
      email: register.email,
      phone: register.phone,
      username: register.username,
      password: register.password,
      confirmPassword: register.confirmPassword
    })) return;
    if (register.password !== register.confirmPassword) {
      setErrors({ confirmPassword: "Passwords must match" });
      return;
    }
    await run("Sending OTP", async () => {
      await api("/auth/register-request", { method: "POST", body: JSON.stringify(register) });
    });
  }

  async function verifyOtp() {
    if (!validate({ otp })) return;
    await run("Creating account", async () => {
      await api("/auth/register-verify", { method: "POST", body: JSON.stringify({ email: register.email, otp }) });
      setLogin({ email: register.email, password: register.password });
      setMessage("Account created. You can sign in now.");
    });
  }

  async function adminSignIn() {
    if (!validate({ adminEmail: adminLogin.email, adminPassword: adminLogin.password })) return;
    await run("Admin sign in", async () => {
      const data = await api("/auth/login", { method: "POST", body: JSON.stringify(adminLogin) });
      setAdminToken(data.token);
      localStorage.setItem("adminToken", data.token);
      await loadAdminData(data.token);
    });
  }

  function logout(type) {
    if (type === "admin") {
      setAdminToken("");
      localStorage.removeItem("adminToken");
      return;
    }
    setCustomerToken("");
    setOrders([]);
    localStorage.removeItem("customerToken");
  }

  async function addToCart(productId) {
    await run("Adding to cart", async () => {
      const data = await api("/cart/items", {
        method: "POST",
        headers: guestCartId && !customerToken ? { "X-Guest-Cart-Id": guestCartId } : {},
        body: JSON.stringify({ productId, quantity: 1 })
      }, customerToken);
      if (!customerToken && data?.cartKey && !data.cartKey.startsWith("user:")) {
        setGuestCartId(data.cartKey);
        localStorage.setItem("guestCartId", data.cartKey);
      }
      setCart(data);
      setPage("cart");
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
    if (!customerToken) {
      setPage("account");
      setMessage("Please sign in before checkout");
      return;
    }
    if (!cart?.items?.length) {
      setMessage("Your cart is empty");
      return;
    }
    if (!validate({
      fullName: checkout.fullName,
      phone: checkout.phone,
      line1: checkout.line1,
      city: checkout.city,
      state: checkout.state,
      postalCode: checkout.postalCode,
      country: checkout.country
    })) return;

    await run("Placing order", async () => {
      const order = await api("/orders/checkout", {
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
      }, customerToken);

      if (checkout.paymentMethod === "CASH_ON_DELIVERY") {
        await Promise.all([loadCart(), loadOrders(), loadProducts()]);
        setPayment({ orderId: "", providerOrderId: "", razorpayPaymentId: "", razorpaySignature: "" });
      } else {
        const pending = await api(`/payments/orders/${order.id}`, {}, customerToken);
        const nextPayment = {
          orderId: String(order.id),
          providerOrderId: pending.providerOrderId || pending.providerReference || "",
          razorpayPaymentId: "",
          razorpaySignature: ""
        };
        setPayment(nextPayment);
        if (frontendConfig.paymentProvider === "razorpay" && frontendConfig.razorpayKeyId) {
          await openRazorpayCheckout(order, pending, nextPayment.providerOrderId);
        } else {
          setPaymentModal({ order, pendingPayment: pending, providerOrderId: nextPayment.providerOrderId });
        }
      }
    });
  }

  async function openRazorpayCheckout(order, pendingPayment, providerOrderId) {
    await loadScript("https://checkout.razorpay.com/v1/checkout.js");
    await new Promise((resolve, reject) => {
      const checkoutInstance = new window.Razorpay({
        key: frontendConfig.razorpayKeyId,
        amount: Math.round(Number(pendingPayment.amount || order.total) * 100),
        currency: "INR",
        name: "ShopSphere",
        description: `Order #${order.id}`,
        order_id: providerOrderId,
        prefill: {
          name: checkout.fullName,
          contact: checkout.phone
        },
        theme: { color: "#126b5c" },
        handler: async (response) => {
          try {
            await api(`/payments/orders/${order.id}/pay`, {
              method: "POST",
              body: JSON.stringify({
                razorpayOrderId: response.razorpay_order_id,
                razorpayPaymentId: response.razorpay_payment_id,
                razorpaySignature: response.razorpay_signature
              })
            }, customerToken);
            setPayment({ orderId: "", providerOrderId: "", razorpayPaymentId: "", razorpaySignature: "" });
            await Promise.all([loadCart(), loadOrders(), loadProducts()]);
            resolve();
          } catch (error) {
            reject(error);
          }
        },
        modal: {
          ondismiss: () => {
            setMessage("Payment was not completed. You can retry from checkout.");
            resolve();
          }
        }
      });
      checkoutInstance.open();
    });
  }

  async function confirmPayment() {
    if (!payment.orderId) return;
    await run("Confirming payment", async () => {
      await api(`/payments/orders/${payment.orderId}/pay`, {
        method: "POST",
        body: JSON.stringify({
          providerToken: "mock-payment-token",
          razorpayOrderId: payment.providerOrderId,
          razorpayPaymentId: payment.razorpayPaymentId,
          razorpaySignature: payment.razorpaySignature
        })
      }, customerToken);
      setPayment({ orderId: "", providerOrderId: "", razorpayPaymentId: "", razorpaySignature: "" });
      await Promise.all([loadCart(), loadOrders(), loadProducts()]);
    });
  }

  async function updateOrderStatus(orderId, action) {
    await run(action === "cancel" ? "Cancelling order" : "Requesting return", async () => {
      await api(`/orders/${orderId}/${action}`, { method: "POST" }, customerToken);
      await Promise.all([loadOrders(), loadProducts()]);
    });
  }

  async function saveBrand() {
    if (!validate({ brandName: brandForm.name })) return;
    await run(brandForm.id ? "Updating brand" : "Creating brand", async () => {
      await api(brandForm.id ? `/admin/brand/${brandForm.id}` : "/admin/brand", {
        method: brandForm.id ? "PUT" : "POST",
        body: JSON.stringify({ name: brandForm.name, logoUrl: brandForm.logoUrl })
      }, adminToken);
      setBrandForm({ id: "", name: "", logoUrl: "" });
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  async function deleteBrand(id) {
    await run("Deleting brand", async () => {
      await api(`/admin/brand/${id}`, { method: "DELETE" }, adminToken);
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  async function saveProduct() {
    if (!validate({
      productName: productForm.name,
      productDescription: productForm.description,
      productPrice: productForm.price,
      productQuantity: productForm.quantity
    })) return;
    await run(productForm.id ? "Updating product" : "Creating product", async () => {
      await api(productForm.id ? `/admin/product/${productForm.id}` : "/admin/product", {
        method: productForm.id ? "PUT" : "POST",
        body: JSON.stringify({
          name: productForm.name,
          description: productForm.description,
          price: Number(productForm.price),
          quantity: Number(productForm.quantity),
          imageUrl: productForm.imageUrl,
          category: productForm.category,
          brand: productForm.brandId ? { id: Number(productForm.brandId) } : null
        })
      }, adminToken);
      setProductForm(emptyProduct);
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  async function deleteProduct(id) {
    await run("Deleting product", async () => {
      await api(`/admin/product/${id}`, { method: "DELETE" }, adminToken);
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  async function updateInventory(product, quantity) {
    const next = Number(quantity);
    if (Number.isNaN(next) || next < 0) {
      setMessage("Inventory must be zero or more");
      return;
    }
    await run("Updating inventory", async () => {
      await api(`/admin/product/${product.id}/inventory?quantity=${next}`, { method: "PUT" }, adminToken);
      await Promise.all([loadAdminData(), loadProducts()]);
    });
  }

  function editProduct(product) {
    setProductForm({
      id: String(product.id),
      name: product.name || "",
      description: product.description || "",
      price: String(product.price || ""),
      quantity: String(product.quantity ?? ""),
      imageUrl: product.imageUrl || "",
      category: product.category || "ELECTRONICS",
      brandId: product.brand?.id ? String(product.brand.id) : ""
    });
  }

  return (
    <main className="appShell">
      <header className="navBar">
        <button className="brandMark" onClick={() => setPage("home")}>
          <PackageCheck size={24} />
          <span>ShopSphere</span>
        </button>
        <div className="searchBox">
          <Search size={18} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search products, brands, categories" />
        </div>
        <nav className="navActions">
          <NavButton active={page === "home"} onClick={() => setPage("home")} icon={<Grid2X2 size={17} />} label="Store" />
          <NavButton active={page === "orders"} onClick={() => setPage(customerToken ? "orders" : "account")} icon={<PackageCheck size={17} />} label="Orders" />
          <NavButton active={page === "account"} onClick={() => setPage("account")} icon={<LogIn size={17} />} label="Account" />
          <NavButton active={page === "admin"} onClick={() => setPage("admin")} icon={<Shield size={17} />} label="Admin" />
          <button className="cartButton" onClick={() => setPage("cart")}>
            <ShoppingCart size={18} />
            <span>{cartCount}</span>
          </button>
        </nav>
      </header>

      {message && (
        <div className="notice">
          <span>{message}</span>
          <button onClick={() => setMessage("")}><X size={16} /></button>
        </div>
      )}

      {page === "home" && (
        <StorePage
          products={filteredProducts}
          allProducts={products}
          category={category}
          setCategory={setCategory}
          addToCart={addToCart}
          setSelectedProduct={setSelectedProduct}
          setPage={setPage}
          customerToken={customerToken}
        />
      )}

      {page === "cart" && (
        <CartPage
          cart={cart}
          cartTotal={cartTotal}
          shippingFee={shippingFee}
          updateCart={updateCart}
          setPage={setPage}
          customerToken={customerToken}
        />
      )}

      {page === "checkout" && (
        <CheckoutPage
          cart={cart}
          cartTotal={cartTotal}
          shippingFee={shippingFee}
          checkout={checkout}
          setCheckout={setCheckout}
          errors={errors}
          placeOrder={placeOrder}
          payment={payment}
          setPayment={setPayment}
          confirmPayment={confirmPayment}
          paymentProvider={frontendConfig.paymentProvider}
        />
      )}

      {page === "account" && (
        <AccountPage
          customerToken={customerToken}
          login={login}
          setLogin={setLogin}
          googleLogin={googleLogin}
          setGoogleLogin={setGoogleLogin}
          register={register}
          setRegister={setRegister}
          otp={otp}
          setOtp={setOtp}
          errors={errors}
          loginCustomer={loginCustomer}
          loginWithGoogle={loginWithGoogle}
          registerRequest={registerRequest}
          verifyOtp={verifyOtp}
          logout={() => logout("customer")}
          googleClientId={frontendConfig.googleClientId}
          googleVerificationEnabled={frontendConfig.googleVerificationEnabled}
          onGoogleCredential={(idToken) => {
            setGoogleLogin((current) => ({ ...current, idToken }));
            loginWithGoogle(idToken);
          }}
        />
      )}

      {page === "orders" && (
        <OrdersPage orders={orders} updateOrderStatus={updateOrderStatus} />
      )}

      {page === "admin" && (
        <AdminPage
          adminToken={adminToken}
          adminLogin={adminLogin}
          setAdminLogin={setAdminLogin}
          adminSignIn={adminSignIn}
          logout={() => logout("admin")}
          brands={brands}
          brandForm={brandForm}
          setBrandForm={setBrandForm}
          saveBrand={saveBrand}
          deleteBrand={deleteBrand}
          productForm={productForm}
          setProductForm={setProductForm}
          saveProduct={saveProduct}
          adminProducts={adminProducts}
          editProduct={editProduct}
          deleteProduct={deleteProduct}
          updateInventory={updateInventory}
          errors={errors}
        />
      )}

      {selectedProduct && (
        <ProductModal product={selectedProduct} onClose={() => setSelectedProduct(null)} addToCart={addToCart} />
      )}

      {paymentModal && (
        <MockPaymentModal
          details={paymentModal}
          onClose={() => setPaymentModal(null)}
          onPay={async () => {
            await confirmPayment();
            setPaymentModal(null);
          }}
        />
      )}

      {busy && <div className="busyBar" />}
    </main>
  );
}

function NavButton({ active, onClick, icon, label }) {
  return (
    <button className={`navButton ${active ? "active" : ""}`} onClick={onClick}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

function StorePage({ products, allProducts, category, setCategory, addToCart, setSelectedProduct, setPage, customerToken }) {
  const featured = allProducts[0];
  return (
    <section className="pageGrid storeGrid">
      <aside className="filterRail">
        <h2>Categories</h2>
        {categories.map((item) => (
          <button className={category === item ? "selected" : ""} key={item} onClick={() => setCategory(item)}>
            {item === "ALL" ? "All Products" : item}
          </button>
        ))}
      </aside>

      <section className="catalog">
        {!customerToken && (
          <div className="loginPrompt">
            <div>
              <strong>Shopping as a guest</strong>
              <span>Add items now, then sign in at checkout to keep your cart.</span>
            </div>
            <button onClick={() => setPage("account")}><LogIn size={16} /> Sign in</button>
          </div>
        )}

        {featured && (
          <div className="dealBand">
            <img src={featured.imageUrl} alt={featured.name} />
            <div>
              <span>Today&apos;s pick</span>
              <h1>{featured.name}</h1>
              <p>{featured.description}</p>
              <strong>{money(featured.price)}</strong>
            </div>
            <button onClick={() => addToCart(featured.id)}><ShoppingCart size={17} /> Add</button>
          </div>
        )}

        <div className="sectionHeader">
          <div>
            <h2>Shop Products</h2>
            <p>{products.length} items available</p>
          </div>
        </div>

        <div className="productGrid">
          {products.map((product) => (
            <article className="productCard" key={product.id}>
              <button className="wishButton" title="Wishlist"><Heart size={16} /></button>
              <img src={product.imageUrl} alt={product.name} onClick={() => setSelectedProduct(product)} />
              <div className="productBody">
                <span>{product.brand?.name || product.category}</span>
                <h3 onClick={() => setSelectedProduct(product)}>{product.name}</h3>
                <p>{product.description}</p>
                <div className="productMeta">
                  <strong>{money(product.price)}</strong>
                  <small>{product.quantity > 0 ? `${product.quantity} left` : "Out of stock"}</small>
                </div>
              </div>
              <button onClick={() => addToCart(product.id)} disabled={product.quantity < 1}>
                <ShoppingCart size={16} /> Add to Cart
              </button>
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}

function CartPage({ cart, cartTotal, shippingFee, updateCart, setPage, customerToken }) {
  return (
    <section className="pageGrid cartGrid">
      <Panel title="Shopping Cart" icon={<ShoppingCart size={19} />}>
        <div className="cartList">
          {(cart?.items || []).map((item) => (
            <div className="cartItem" key={item.productId}>
              <div>
                <h3>{item.productName}</h3>
                <span>{money(item.unitPrice)} each</span>
              </div>
              <div className="quantityControls">
                <button onClick={() => updateCart(item.productId, item.quantity - 1)}><Minus size={14} /></button>
                <strong>{item.quantity}</strong>
                <button onClick={() => updateCart(item.productId, item.quantity + 1)}><Plus size={14} /></button>
              </div>
              <strong>{money(item.lineTotal)}</strong>
            </div>
          ))}
          {!cart?.items?.length && <EmptyState title="Your cart is empty" />}
        </div>
      </Panel>
      <OrderSummary cartTotal={cartTotal} shippingFee={shippingFee}>
        <button className="primaryAction" onClick={() => setPage(customerToken ? "checkout" : "account")} disabled={!cart?.items?.length}>
          <CreditCard size={17} /> Checkout
        </button>
      </OrderSummary>
    </section>
  );
}

function CheckoutPage({ cart, cartTotal, shippingFee, checkout, setCheckout, errors, placeOrder, payment, paymentProvider }) {
  return (
    <section className="pageGrid checkoutGrid">
      <Panel title="Shipping Address" icon={<CreditCard size={19} />}>
        <div className="formGrid two">
          <Field name="fullName" placeholder="Full name" value={checkout.fullName} error={errors.fullName} onChange={(value) => setCheckout({ ...checkout, fullName: value })} />
          <Field name="phone" placeholder="Phone" value={checkout.phone} error={errors.phone} onChange={(value) => setCheckout({ ...checkout, phone: value })} />
          <Field name="line1" placeholder="Address line 1" value={checkout.line1} error={errors.line1} onChange={(value) => setCheckout({ ...checkout, line1: value })} />
          <Field name="line2" placeholder="Address line 2" value={checkout.line2} onChange={(value) => setCheckout({ ...checkout, line2: value })} />
          <Field name="city" placeholder="City" value={checkout.city} error={errors.city} onChange={(value) => setCheckout({ ...checkout, city: value })} />
          <Field name="state" placeholder="State" value={checkout.state} error={errors.state} onChange={(value) => setCheckout({ ...checkout, state: value })} />
          <Field name="postalCode" placeholder="Postal code" value={checkout.postalCode} error={errors.postalCode} onChange={(value) => setCheckout({ ...checkout, postalCode: value })} />
          <Field name="country" placeholder="Country" value={checkout.country} error={errors.country} onChange={(value) => setCheckout({ ...checkout, country: value })} />
          <label className="field">
            <span>Payment</span>
            <select value={checkout.paymentMethod} onChange={(event) => setCheckout({ ...checkout, paymentMethod: event.target.value })}>
              <option>CARD</option>
              <option>UPI</option>
              <option>NET_BANKING</option>
              <option>CASH_ON_DELIVERY</option>
            </select>
          </label>
        </div>
      </Panel>

      <OrderSummary cartTotal={cartTotal} shippingFee={shippingFee}>
        <button className="primaryAction" onClick={placeOrder} disabled={!cart?.items?.length}>
          <BadgeIndianRupee size={17} /> Place Order
        </button>
      </OrderSummary>

      {payment.orderId && paymentProvider === "razorpay" && (
        <Panel title="Payment Pending" icon={<BadgeIndianRupee size={19} />}>
          <div className="paymentBox">
            <span>Razorpay Checkout opened for this order. If you dismissed it, place the order again after cancelling this pending order.</span>
          </div>
        </Panel>
      )}
    </section>
  );
}

function AccountPage({ customerToken, login, setLogin, googleLogin, setGoogleLogin, register, setRegister, otp, setOtp, errors, loginCustomer, loginWithGoogle, registerRequest, verifyOtp, logout, googleClientId, googleVerificationEnabled, onGoogleCredential }) {
  return (
    <section className="pageGrid accountGrid">
      <Panel title="Customer Sign In" icon={<LogIn size={19} />}>
        {customerToken ? (
          <div className="signedIn">
            <CheckCircle2 size={28} />
            <strong>Customer session active</strong>
            <button onClick={logout}><LogOut size={16} /> Sign out</button>
          </div>
        ) : (
          <div className="formGrid">
            <Field name="email" placeholder="Email" value={login.email} error={errors.email} onChange={(value) => setLogin({ ...login, email: value })} />
            <Field name="password" placeholder="Password" type="password" value={login.password} error={errors.password} onChange={(value) => setLogin({ ...login, password: value })} />
            <button className="primaryAction" onClick={loginCustomer}>Sign In</button>
          </div>
        )}
      </Panel>

      <Panel title="Google OAuth" icon={<LogIn size={19} />}>
        <div className="formGrid">
          {googleClientId ? (
            <GoogleSignInButton clientId={googleClientId} onCredential={onGoogleCredential} />
          ) : (
            <button className="googleFallback" disabled>
              <span className="googleGlyph">G</span>
              Configure Google Client ID
            </button>
          )}
          {!googleVerificationEnabled && !googleClientId && (
            <>
              <Field name="googleEmail" placeholder="Demo Google email" value={googleLogin.email} error={errors.email} onChange={(value) => setGoogleLogin({ ...googleLogin, email: value })} />
              <Field name="googleName" placeholder="Display name" value={googleLogin.name} onChange={(value) => setGoogleLogin({ ...googleLogin, name: value })} />
              <button onClick={() => loginWithGoogle()}>Use Demo Google Login</button>
            </>
          )}
        </div>
      </Panel>

      <Panel title="Create Account" icon={<UserPlus size={19} />} wide>
        <div className="formGrid two">
          <Field name="registerEmail" placeholder="Email" value={register.email} error={errors.email} onChange={(value) => setRegister({ ...register, email: value })} />
          <Field name="phone" placeholder="Phone" value={register.phone} error={errors.phone} onChange={(value) => setRegister({ ...register, phone: value })} />
          <Field name="username" placeholder="Name" value={register.username} error={errors.username} onChange={(value) => setRegister({ ...register, username: value })} />
          <Field name="registerPassword" placeholder="Password" type="password" value={register.password} error={errors.password} onChange={(value) => setRegister({ ...register, password: value })} />
          <Field name="confirmPassword" placeholder="Confirm password" type="password" value={register.confirmPassword} error={errors.confirmPassword} onChange={(value) => setRegister({ ...register, confirmPassword: value })} />
          <button onClick={registerRequest}>Send OTP</button>
          <Field name="otp" placeholder="OTP" value={otp} error={errors.otp} onChange={setOtp} />
          <button onClick={verifyOtp}>Verify Account</button>
        </div>
      </Panel>
    </section>
  );
}

function GoogleSignInButton({ clientId, onCredential }) {
  useEffect(() => {
    let cancelled = false;
    loadScript("https://accounts.google.com/gsi/client")
      .then(() => {
        if (cancelled || !window.google?.accounts?.id) return;
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => onCredential(response.credential)
        });
        window.google.accounts.id.renderButton(
          document.getElementById("googleSignInButton"),
          { theme: "outline", size: "large", width: 280 }
        );
        window.google.accounts.id.prompt();
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [clientId]);

  return <div id="googleSignInButton" className="googleButton" />;
}

function OrdersPage({ orders, updateOrderStatus }) {
  return (
    <section className="pageGrid">
      <Panel title="Your Orders" icon={<PackageCheck size={19} />} wide>
        <div className="orderList">
          {orders.map((order) => (
            <div className="orderCard" key={order.id}>
              <div>
                <strong>Order #{order.id}</strong>
                <span>{order.status}</span>
              </div>
              <div>
                <strong>{money(order.total)}</strong>
                <span>{order.items.length} items</span>
              </div>
              <div className="rowActions">
                {["PAYMENT_PENDING", "PAID", "CONFIRMED"].includes(order.status) && (
                  <button onClick={() => updateOrderStatus(order.id, "cancel")}>Cancel</button>
                )}
                {["PAID", "CONFIRMED"].includes(order.status) && (
                  <button onClick={() => updateOrderStatus(order.id, "return")}>Return</button>
                )}
              </div>
            </div>
          ))}
          {!orders.length && <EmptyState title="No orders yet" />}
        </div>
      </Panel>
    </section>
  );
}

function AdminPage({ adminToken, adminLogin, setAdminLogin, adminSignIn, logout, brands, brandForm, setBrandForm, saveBrand, deleteBrand, productForm, setProductForm, saveProduct, adminProducts, editProduct, deleteProduct, updateInventory, errors }) {
  return (
    <section className="pageGrid adminGrid">
      <Panel title="Admin Access" icon={<Shield size={19} />}>
        {adminToken ? (
          <div className="signedIn">
            <Shield size={28} />
            <strong>Admin session active</strong>
            <button onClick={logout}><LogOut size={16} /> Sign out</button>
          </div>
        ) : (
          <div className="formGrid">
            <Field name="adminEmail" placeholder="Admin email" value={adminLogin.email} error={errors.adminEmail} onChange={(value) => setAdminLogin({ ...adminLogin, email: value })} />
            <Field name="adminPassword" placeholder="Admin password" type="password" value={adminLogin.password} error={errors.adminPassword} onChange={(value) => setAdminLogin({ ...adminLogin, password: value })} />
            <button className="primaryAction" onClick={adminSignIn}>Admin Sign In</button>
          </div>
        )}
      </Panel>

      <Panel title="Brands" icon={<Boxes size={19} />}>
        <div className="formGrid">
          <Field name="brandName" placeholder="Brand name" value={brandForm.name} error={errors.brandName} onChange={(value) => setBrandForm({ ...brandForm, name: value })} />
          <Field name="brandLogo" placeholder="Logo URL" value={brandForm.logoUrl} onChange={(value) => setBrandForm({ ...brandForm, logoUrl: value })} />
          <button onClick={saveBrand} disabled={!adminToken}>{brandForm.id ? "Update Brand" : "Add Brand"}</button>
        </div>
        <div className="manageList">
          {brands.map((brand) => (
            <div className="manageRow" key={brand.id}>
              <div>
                <strong>{brand.name}</strong>
                <span>#{brand.id}</span>
              </div>
              <div className="rowActions">
                <button onClick={() => setBrandForm({ id: String(brand.id), name: brand.name || "", logoUrl: brand.logoUrl || "" })}><Edit3 size={15} /></button>
                <button className="danger" onClick={() => deleteBrand(brand.id)}><Trash2 size={15} /></button>
              </div>
            </div>
          ))}
        </div>
      </Panel>

      <Panel title="Products" icon={<PackagePlus size={19} />} wide>
        <div className="formGrid two">
          <Field name="productName" placeholder="Product name" value={productForm.name} error={errors.productName} onChange={(value) => setProductForm({ ...productForm, name: value })} />
          <Field name="productDescription" placeholder="Description" value={productForm.description} error={errors.productDescription} onChange={(value) => setProductForm({ ...productForm, description: value })} />
          <Field name="productPrice" placeholder="Price" type="number" value={productForm.price} error={errors.productPrice} onChange={(value) => setProductForm({ ...productForm, price: value })} />
          <Field name="productQuantity" placeholder="Stock" type="number" value={productForm.quantity} error={errors.productQuantity} onChange={(value) => setProductForm({ ...productForm, quantity: value })} />
          <Field name="productImage" placeholder="Image URL" value={productForm.imageUrl} onChange={(value) => setProductForm({ ...productForm, imageUrl: value })} />
          <label className="field">
            <span>Brand</span>
            <select value={productForm.brandId} onChange={(event) => setProductForm({ ...productForm, brandId: event.target.value })}>
              <option value="">No brand</option>
              {brands.map((brand) => <option value={brand.id} key={brand.id}>{brand.name}</option>)}
            </select>
          </label>
          <label className="field">
            <span>Category</span>
            <select value={productForm.category} onChange={(event) => setProductForm({ ...productForm, category: event.target.value })}>
              {categories.filter((item) => item !== "ALL").map((item) => <option key={item}>{item}</option>)}
            </select>
          </label>
          <button onClick={saveProduct} disabled={!adminToken}>{productForm.id ? "Update Product" : "Add Product"}</button>
        </div>

        <div className="manageList">
          {adminProducts.map((product) => (
            <div className="productManageRow" key={product.id}>
              <img src={product.imageUrl} alt={product.name} />
              <div>
                <strong>{product.name}</strong>
                <span>{money(product.price)} · {product.brand?.name || "No brand"}</span>
              </div>
              <input defaultValue={product.quantity} min="0" type="number" onBlur={(event) => updateInventory(product, event.target.value)} />
              <div className="rowActions">
                <button onClick={() => editProduct(product)}><Edit3 size={15} /></button>
                <button className="danger" onClick={() => deleteProduct(product.id)}><Trash2 size={15} /></button>
              </div>
            </div>
          ))}
        </div>
      </Panel>
    </section>
  );
}

function ProductModal({ product, onClose, addToCart }) {
  return (
    <div className="modalBackdrop">
      <article className="productModal">
        <button className="modalClose" onClick={onClose}><X size={18} /></button>
        <img src={product.imageUrl} alt={product.name} />
        <div>
          <span>{product.brand?.name || product.category}</span>
          <h2>{product.name}</h2>
          <p>{product.description}</p>
          <strong>{money(product.price)}</strong>
          <small>{product.quantity} in stock</small>
          <button className="primaryAction" onClick={() => addToCart(product.id)} disabled={product.quantity < 1}>
            <ShoppingCart size={17} /> Add to Cart
          </button>
        </div>
      </article>
    </div>
  );
}

function MockPaymentModal({ details, onClose, onPay }) {
  return (
    <div className="modalBackdrop">
      <article className="paymentModal">
        <button className="modalClose" onClick={onClose}><X size={18} /></button>
        <div className="paymentHeader">
          <BadgeIndianRupee size={28} />
          <div>
            <span>Secure demo payment</span>
            <h2>Complete your payment</h2>
          </div>
        </div>
        <div className="paymentRows">
          <div><span>Order</span><strong>#{details.order.id}</strong></div>
          <div><span>Provider order</span><strong>{details.providerOrderId}</strong></div>
          <div><span>Amount</span><strong>{money(details.pendingPayment.amount || details.order.total)}</strong></div>
        </div>
        <div className="demoCard">
          <span>Demo Card</span>
          <strong>4111 1111 1111 1111</strong>
          <small>Use real Razorpay config to show Razorpay Checkout here.</small>
        </div>
        <button className="primaryAction" onClick={onPay}><CheckCircle2 size={17} /> Pay Securely</button>
      </article>
    </div>
  );
}

function OrderSummary({ cartTotal, shippingFee, children }) {
  return (
    <aside className="summaryCard">
      <h2>Order Summary</h2>
      <div><span>Items</span><strong>{money(cartTotal)}</strong></div>
      <div><span>Shipping</span><strong>{money(shippingFee)}</strong></div>
      <div className="summaryTotal"><span>Total</span><strong>{money(cartTotal + shippingFee)}</strong></div>
      {children}
    </aside>
  );
}

function Panel({ title, icon, children, wide = false }) {
  return (
    <section className={`panel ${wide ? "wide" : ""}`}>
      <div className="panelTitle">
        {icon}
        <h2>{title}</h2>
      </div>
      {children}
    </section>
  );
}

function Field({ name, placeholder, value, onChange, error, type = "text" }) {
  return (
    <label className={`field ${error ? "hasError" : ""}`}>
      <span>{placeholder}</span>
      <input name={name} type={type} value={value} onChange={(event) => onChange(event.target.value)} />
      {error && <small>{error}</small>}
    </label>
  );
}

function EmptyState({ title }) {
  return (
    <div className="emptyState">
      <Boxes size={28} />
      <strong>{title}</strong>
    </div>
  );
}

createRoot(document.getElementById("root")).render(<App />);
