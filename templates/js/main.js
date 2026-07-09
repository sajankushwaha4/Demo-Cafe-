document.addEventListener('DOMContentLoaded', () => {
    // ----------------- FLASH MESSAGES AUTO-CLOSE -----------------
    const flashMessages = document.querySelectorAll('.flash-message');
    flashMessages.forEach(msg => {
        const closeBtn = msg.querySelector('.flash-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', () => msg.remove());
        }
        // Auto remove after 4 seconds
        setTimeout(() => {
            msg.style.opacity = '0';
            msg.style.transform = 'translateX(50px)';
            msg.style.transition = 'all 0.5s ease';
            setTimeout(() => msg.remove(), 500);
        }, 4000);
    });

    // ----------------- LOGIN / REGISTER SWITCHING -----------------
    const authTabs = document.querySelectorAll('.auth-tab');
    if (authTabs.length > 0) {
        authTabs.forEach(tab => {
            tab.addEventListener('click', () => {
                const target = tab.dataset.target;
                
                // Toggle active class on tabs
                authTabs.forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                
                // Toggle active class on forms
                document.querySelectorAll('.auth-form').forEach(form => {
                    form.classList.remove('active');
                });
                document.getElementById(`${target}-form`).classList.add('active');
            });
        });
    }

    // ----------------- SHOPPING CART STATE MANAGEMENT -----------------
    let cart = JSON.parse(localStorage.getItem('cafe_cart')) || [];
    
    // Select cart elements
    const cartItemsContainer = document.getElementById('cart-items');
    const cartCountBadge = document.getElementById('cart-badge');
    const cartSubtotalEl = document.getElementById('cart-subtotal');
    const cartGstEl = document.getElementById('cart-gst');
    const cartTotalEl = document.getElementById('cart-total');
    const checkoutBtn = document.getElementById('checkout-btn');
    
    // Function to save cart to localStorage
    const saveCart = () => {
        localStorage.setItem('cafe_cart', JSON.stringify(cart));
        updateCartBadge();
    };

    // Update cart count badge in navbar (if present)
    const updateCartBadge = () => {
        const totalItems = cart.reduce((sum, item) => sum + item.quantity, 0);
        if (cartCountBadge) {
            cartCountBadge.textContent = totalItems;
            cartCountBadge.style.display = totalItems > 0 ? 'inline-block' : 'none';
        }
        const panelCountBadge = document.querySelector('.cart-count-badge');
        if (panelCountBadge) {
            panelCountBadge.textContent = totalItems;
        }
    };

    // Render cart sidebar on menu page
    const renderCart = () => {
        if (!cartItemsContainer) return; // Not on Menu page
        
        cartItemsContainer.innerHTML = '';
        
        if (cart.length === 0) {
            cartItemsContainer.innerHTML = `
                <div style="text-align: center; color: #718096; margin-top: 3rem;">
                    <span style="font-size: 3rem; display: block; margin-bottom: 1rem;">🛒</span>
                    Your cart is empty. Add some delicious items!
                </div>
            `;
            if (cartSubtotalEl) cartSubtotalEl.textContent = 'INR 0.00';
            if (cartGstEl) cartGstEl.textContent = 'INR 0.00';
            if (cartTotalEl) cartTotalEl.textContent = 'INR 0.00';
            if (checkoutBtn) checkoutBtn.disabled = true;
            return;
        }
        
        let subtotal = 0;
        
        cart.forEach(item => {
            const itemTotal = item.price * item.quantity;
            subtotal += itemTotal;
            
            const itemEl = document.createElement('div');
            itemEl.className = 'cart-item';
            itemEl.innerHTML = `
                <div class="cart-item-detail">
                    <div class="cart-item-name">${item.name}</div>
                    <div class="cart-item-price">INR ${item.price.toFixed(2)}</div>
                    <div class="cart-item-qty">
                        <button class="qty-btn dec-qty" data-id="${item.id}">-</button>
                        <span class="qty-number">${item.quantity}</span>
                        <button class="qty-btn inc-qty" data-id="${item.id}">+</button>
                        <button class="cart-item-remove" data-id="${item.id}">Remove</button>
                    </div>
                </div>
                <div style="font-weight: 600; font-size: 0.95rem;">
                    INR ${itemTotal.toFixed(2)}
                </div>
            `;
            cartItemsContainer.appendChild(itemEl);
        });
        
        const gst = subtotal * 0.05; // 5% GST
        const total = subtotal + gst;
        
        if (cartSubtotalEl) cartSubtotalEl.textContent = `INR ${subtotal.toFixed(2)}`;
        if (cartGstEl) cartGstEl.textContent = `INR ${gst.toFixed(2)}`;
        if (cartTotalEl) cartTotalEl.textContent = `INR ${total.toFixed(2)}`;
        if (checkoutBtn) checkoutBtn.disabled = false;
        
        // Bind item control events
        document.querySelectorAll('.inc-qty').forEach(btn => {
            btn.addEventListener('click', () => updateQuantity(parseInt(btn.dataset.id), 1));
        });
        
        document.querySelectorAll('.dec-qty').forEach(btn => {
            btn.addEventListener('click', () => updateQuantity(parseInt(btn.dataset.id), -1));
        });
        
        document.querySelectorAll('.cart-item-remove').forEach(btn => {
            btn.addEventListener('click', () => removeFromCart(parseInt(btn.dataset.id)));
        });
    };

    // Add item to cart
    const addToCart = (id, name, price) => {
        const existingItem = cart.find(item => item.id === id);
        if (existingItem) {
            existingItem.quantity += 1;
        } else {
            cart.push({ id, name, price, quantity: 1 });
        }
        saveCart();
        renderCart();
        showNotification(`${name} added to cart!`, 'success');
    };

    // Update item quantity
    const updateQuantity = (id, change) => {
        const item = cart.find(item => item.id === id);
        if (item) {
            item.quantity += change;
            if (item.quantity <= 0) {
                removeFromCart(id);
                return;
            }
            saveCart();
            renderCart();
        }
    };

    // Remove item from cart
    const removeFromCart = (id) => {
        const item = cart.find(item => item.id === id);
        cart = cart.filter(item => item.id !== id);
        saveCart();
        renderCart();
        if (item) {
            showNotification(`${item.name} removed from cart.`, 'info');
        }
    };

    // Show temporary JS notifications
    const showNotification = (message, type = 'success') => {
        const container = document.querySelector('.flash-messages') || (() => {
            const div = document.createElement('div');
            div.className = 'flash-messages';
            document.body.appendChild(div);
            return div;
        })();
        
        const notification = document.createElement('div');
        notification.className = `flash-message ${type}`;
        notification.innerHTML = `
            <span>${message}</span>
            <button class="flash-close">&times;</button>
        `;
        container.appendChild(notification);
        
        notification.querySelector('.flash-close').addEventListener('click', () => {
            notification.remove();
        });
        
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transform = 'translateX(50px)';
            notification.style.transition = 'all 0.5s ease';
            setTimeout(() => notification.remove(), 500);
        }, 3000);
    };

    // Bind Add to Cart buttons on menu cards
    document.querySelectorAll('.add-to-cart-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.dataset.id);
            const name = btn.dataset.name;
            const price = parseFloat(btn.dataset.price);
            addToCart(id, name, price);
        });
    });

    // ----------------- CART CHECKOUT SUBMISSION -----------------
    const checkoutForm = document.getElementById('mini-checkout-form');
    if (checkoutForm) {
        checkoutForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            if (cart.length === 0) {
                showNotification('Your cart is empty.', 'error');
                return;
            }
            
            const customerName = document.getElementById('cust-name').value.strip;
            const customerPhone = document.getElementById('cust-phone').value.strip;
            
            const payload = {
                customer_name: document.getElementById('cust-name').value,
                customer_phone: document.getElementById('cust-phone').value,
                cart: cart
            };
            
            try {
                const response = await fetch('/api/orders/create', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                
                const result = await response.json();
                
                if (result.success) {
                    // Clear cart
                    cart = [];
                    saveCart();
                    // Redirect to checkout QR page
                    window.location.href = `/checkout?order_id=${result.order_id}`;
                } else {
                    showNotification(result.message || 'Failed to place order.', 'error');
                    if (response.status === 401) {
                        // Redirect to login after a short delay
                        setTimeout(() => window.location.href = '/login', 1500);
                    }
                }
            } catch (error) {
                console.error('Checkout error:', error);
                showNotification('Connection error. Please try again.', 'error');
            }
        });
    }

    // ----------------- RAZORPAY PAYMENT GATEWAY INTEGRATION -----------------
    const razorpayPayBtn = document.getElementById('razorpay-pay-btn');
    
    if (razorpayPayBtn) {
        const orderId = razorpayPayBtn.dataset.orderId;
        
        razorpayPayBtn.addEventListener('click', async () => {
            razorpayPayBtn.disabled = true;
            razorpayPayBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Preparing Payment...';
            
            try {
                // 1. Call backend to create Razorpay Order
                const res = await fetch(`/api/payment/create-order/${orderId}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' }
                });
                
                const data = await res.json();
                
                if (!data.success) {
                    showNotification(data.message || 'Failed to initialize payment gateway.', 'error');
                    resetPayBtnState();
                    return;
                }
                
                // 2. Configure Razorpay Checkout options
                const options = {
                    key: data.key_id,
                    amount: data.amount,
                    currency: data.currency,
                    name: 'Kushwaha Cafe',
                    description: `Invoice Bill payment for Order KC-ORD-${orderId}`,
                    image: 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?q=80&w=150&auto=format&fit=crop',
                    order_id: data.razorpay_order_id,
                    handler: async function (response) {
                        // 3. Send verification credentials to backend
                        razorpayPayBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Verifying Payment...';
                        
                        try {
                            const verifyRes = await fetch('/api/payment/verify', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({
                                    razorpay_order_id: response.razorpay_order_id,
                                    razorpay_payment_id: response.razorpay_payment_id,
                                    razorpay_signature: response.razorpay_signature,
                                    local_order_id: parseInt(orderId)
                                })
                            });
                            
                            const verifyData = await verifyRes.json();
                            
                            if (verifyData.success) {
                                showNotification('Payment Successful & Verified!', 'success');
                                razorpayPayBtn.innerHTML = '<i class="fa-solid fa-circle-check"></i> Success!';
                                setTimeout(() => {
                                    window.location.href = `/bill/${orderId}`;
                                }, 1000);
                            } else {
                                showNotification(verifyData.message || 'Verification failed.', 'error');
                                resetPayBtnState();
                            }
                        } catch (err) {
                            console.error('Verify error:', err);
                            showNotification('Verification connection error.', 'error');
                            resetPayBtnState();
                        }
                    },
                    prefill: {
                        name: data.customer_name || '',
                        contact: data.customer_phone || '',
                        email: data.customer_email || ''
                    },
                    notes: {
                        local_order_id: orderId
                    },
                    theme: {
                        color: '#2A1A10'
                    },
                    modal: {
                        ondismiss: function() {
                            showNotification('Payment cancelled by user.', 'info');
                            resetPayBtnState();
                        }
                    }
                };
                
                // 4. Open Razorpay payment modal
                const rzp = new Razorpay(options);
                rzp.open();
                
            } catch (error) {
                console.error('Payment initialization error:', error);
                showNotification('Connection error. Please try again.', 'error');
                resetPayBtnState();
            }
        });
        
        const resetPayBtnState = () => {
            razorpayPayBtn.disabled = false;
            razorpayPayBtn.innerHTML = '<i class="fa-solid fa-bolt"></i> Pay with Razorpay';
        };
    }

    // ----------------- PRINT RECEIPT -----------------
    const printBillBtn = document.getElementById('print-bill-btn');
    if (printBillBtn) {
        printBillBtn.addEventListener('click', () => {
            window.print();
        });
    }

    // Initialize Menu page cart on load
    updateCartBadge();
    renderCart();
});
