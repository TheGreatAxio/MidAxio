async function loadComponents() {
    const components = {
        'global-nav': 'components/navbar.html',
        'side-sidebar': 'components/sidebar.html',
        'ad-footer': 'components/footer.html',
        'region-selector-container': 'components/region-selector.html' // New entry
    };

    for (const [className, filePath] of Object.entries(components)) {
        const element = document.querySelector(`.${className}`);
        if (element) {
            try {
                const response = await fetch(filePath);
                if (response.ok) {
                    const html = await response.text();
                    element.innerHTML = html;
                }
            } catch (err) {
                console.error(err);
            }
        }
    }
}

function updateAuthUI() {
    const token = localStorage.getItem('token');
    const path = window.location.pathname;

    const isAuthPage = path.includes('login.html') || path.includes('signup.html');
    const isAccountPage = path.includes('account.html');

    const loggedOutLinks = document.getElementById('loggedOutLinks');
    const loggedInLinks = document.getElementById('loggedInLinks');

    if (isAuthPage || isAccountPage) {
        if (loggedOutLinks) loggedOutLinks.style.display = 'none';
        if (loggedInLinks) loggedInLinks.style.display = 'none';
    } else {
        if (token) {
            if (loggedOutLinks) loggedOutLinks.style.display = 'none';
            if (loggedInLinks) loggedInLinks.style.display = 'flex';
        } else {
            if (loggedOutLinks) loggedOutLinks.style.display = 'flex';
            if (loggedInLinks) loggedInLinks.style.display = 'none';
        }
    }

    const authZone = document.getElementById('sidebarAuthZone');
    if (authZone && token) {
        authZone.innerHTML = `
            <div class="sidebar-item" id="logoutBtn" title="Logout">
                <span class="icon">🚪</span>
                <span class="label">Logout</span>
            </div>
        `;
        document.getElementById('logoutBtn').addEventListener('click', () => {
            localStorage.removeItem('token');
            window.location.href = 'index.html';
        });
    } else if (authZone) {
        authZone.innerHTML = '';
    }
}

const signupForm = document.getElementById('signupForm');
if (signupForm) {
    signupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const messageElement = document.getElementById('message');

        try {
            const response = await fetch('https://api.axioscomputers.com/api/v1/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await response.json();
            messageElement.innerText = response.ok ? "Check your email to verify!" : (data.message || "Registration failed.");
            messageElement.style.color = response.ok ? "lightgreen" : "red";
        } catch (error) {
            messageElement.innerText = "Error connecting to server.";
        }
    });
}

const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        const messageElement = document.getElementById('loginMessage');

        try {
            const response = await fetch('https://api.axioscomputers.com/api/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            const data = await response.json();
            if (response.ok) {
                localStorage.setItem('token', data.token);
                messageElement.style.color = "lightgreen";
                messageElement.innerText = "Login Successful!";
                setTimeout(() => { window.location.href = 'index.html'; }, 1000);
            } else {
                messageElement.style.color = "red";
                messageElement.innerText = "Login failed: Check credentials.";
            }
        } catch (error) {
            messageElement.innerText = "Error connecting to server.";
        }
    });
}

async function fetchUserProfile() {
    const profileDiv = document.getElementById('userProfile');
    if (!profileDiv) return;

    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }

    try {
        const response = await fetch('https://api.axioscomputers.com/api/v1/user/me', {
            method: 'GET',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const user = await response.json();

            const riotStatusHTML = user.riotLinked
                ? `<span style="color: lightgreen;">✅ Linked (${user.gameName}#${user.tagLine})</span>`
                : `<span style="color: orange;">❌ Not Linked</span>`;

            profileDiv.innerHTML = `
                <div class="profile-details">
                    <p><strong>Email:</strong> ${user.email}</p>
                    <p><strong>Riot Account:</strong> ${riotStatusHTML}</p>
                </div>
            `;

            if (user.riotLinked) {
                const vSection = document.querySelector('.verification-section');
                if (vSection) vSection.style.display = 'none';
            }
        } else if (response.status === 401) {
            localStorage.removeItem('token');
            window.location.href = 'login.html';
        }
    } catch (error) {
        profileDiv.innerHTML = `<p style="color: red;">Error loading profile data.</p>`;
    }
}

const verifyBtn = document.getElementById('verifyBtn');
if (verifyBtn) {
    verifyBtn.addEventListener('click', async () => {
        const gameName = document.getElementById('riotName').value;
        const tagLine = document.getElementById('riotTag').value;

        const leagueRegion = document.getElementById('regionSelect').value;

        const messageElement = document.getElementById('verifyMessage');
        const token = localStorage.getItem('token');

        try {
            messageElement.innerText = "Initiating...";

            const response = await fetch('https://api.axioscomputers.com/api/v1/user/ign/initiate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ gameName, tagLine, leagueRegion })
            });

            const text = await response.text();

            if (response.ok) {
                messageElement.style.color = "orange";
                messageElement.innerText = text;

                verifyBtn.innerText = "Confirm Icon Change";
                verifyBtn.onclick = confirmVerification;
            } else {
                messageElement.style.color = "red";
                messageElement.innerText = text || "Initiation failed.";
            }
        } catch (error) {
            messageElement.innerText = "Error connecting to server.";
        }
    });
}

async function confirmVerification() {
    const token = localStorage.getItem('token');
    const messageElement = document.getElementById('verifyMessage');

    try {
        const response = await fetch('https://api.axioscomputers.com/api/v1/user/ign/confirm', {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            messageElement.style.color = "lightgreen";
            messageElement.innerText = "Success! Account linked.";
            setTimeout(() => location.reload(), 1500);
        } else {
            const errorText = await response.text();
            messageElement.innerText = errorText || "Icon mismatch. Try again.";
        }
    } catch (error) {
        messageElement.innerText = "Error connecting to server.";
    }
}

window.addEventListener('DOMContentLoaded', () => {
    loadComponents();
    fetchUserProfile();
});