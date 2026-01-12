async function loadComponents() {
    const components = {
        'global-nav': 'components/navbar.html',
        'side-sidebar': 'components/sidebar.html',
        'ad-footer': 'components/footer.html',
        'region-selector-container': 'components/region-selector.html'
    };

    const promises = Object.entries(components).map(async ([className, filePath]) => {
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
    });

    await Promise.all(promises);
    updateAuthUI();
    setupSearch();
}

function updateAuthUI() {
    const token = localStorage.getItem('token');
    const path = window.location.pathname;
    const loggedOutLinks = document.getElementById('loggedOutLinks');
    const loggedInLinks = document.getElementById('loggedInLinks');
    const isAuthPage = path.includes('login') || path.includes('signup');

    if (isAuthPage) {
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
            window.location.href = 'index';
        });
    } else if (authZone) {
        authZone.innerHTML = '';
    }
}

const signupForm = document.getElementById('signupForm');
if (signupForm) {
    signupForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('username').value;
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const messageElement = document.getElementById('message');

        try {
            const response = await fetch('https://api.axioscomputers.com/api/v1/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
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
        const identifier = document.getElementById('loginIdentifier').value;
        const password = document.getElementById('loginPassword').value;
        const messageElement = document.getElementById('loginMessage');

        try {
            const response = await fetch('https://api.axioscomputers.com/api/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ identifier, password })
            });

            const responseText = await response.text();
            let data;
            try {
                data = JSON.parse(responseText);
            } catch (err) {
                data = { message: responseText };
            }

            if (response.ok) {
                localStorage.setItem('token', data.token);
                messageElement.style.color = "lightgreen";
                messageElement.innerText = "Login Successful!";
                setTimeout(() => { window.location.href = 'index'; }, 1000);
            } else {
                messageElement.style.color = "red";
                messageElement.innerText = data.message || data.error || responseText || "Login failed.";
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
        window.location.href = 'login';
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
            window.location.href = 'login';
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
                const iconId = text.match(/\d+/)[0];
                const iconUrl = `https://ddragon.leagueoflegends.com/cdn/26.01.1/img/profileicon/${iconId}.png`;

                messageElement.style.color = "white";
                messageElement.innerHTML = `
                    <div style="margin: 15px 0; text-align: center;">
                        <p style="color: orange; font-weight: bold;">Action Required:</p>
                        <p>Change your League icon to this picture:</p>
                        <img src="${iconUrl}" alt="Target Icon" style="width: 80px; height: 80px; border: 2px solid #4169e1; border-radius: 8px; margin: 10px 0; box-shadow: 0 4px 10px rgba(0,0,0,0.5);">
                        <p style="font-size: 0.85rem; color: #aaa;">Once changed, click the button below.</p>
                    </div>
                `;

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

let searchTimeout = null;
let currentSelectionIndex = -1;

function setupSearch() {
    const searchInput = document.getElementById('globalSearchInput');
    const searchContainer = document.querySelector('.nav-search');
    const dropdown = document.getElementById('searchDropdown');
    const searchBtn = document.getElementById('globalSearchBtn');

    if (!searchInput || !searchContainer || !dropdown) return;

    searchInput.addEventListener('input', (e) => {
        const query = e.target.value.trim();
        clearTimeout(searchTimeout);

        if (query.length < 2) {
            closeDropdown();
            return;
        }

        searchTimeout = setTimeout(() => performSearch(query), 300);
    });

    searchInput.addEventListener('keydown', (e) => {
        const items = document.querySelectorAll('.search-item');
        if (items.length === 0) return;

        if (e.key === 'ArrowDown') {
            e.preventDefault();
            currentSelectionIndex++;
            if (currentSelectionIndex >= items.length) currentSelectionIndex = 0;
            updateSelection(items);
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            currentSelectionIndex--;
            if (currentSelectionIndex < 0) currentSelectionIndex = items.length - 1;
            updateSelection(items);
        } else if (e.key === 'Enter') {
            e.preventDefault();
            if (currentSelectionIndex > -1) {
                items[currentSelectionIndex].click();
            } else {
                performSearch(searchInput.value, true);
            }
        } else if (e.key === 'Escape') {
            closeDropdown();
        }
    });

    document.addEventListener('click', (e) => {
        if (!searchContainer.contains(e.target)) {
            closeDropdown();
        }
    });

    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            performSearch(searchInput.value, true);
        });
    }
}

async function performSearch(query, forceRedirect = false) {
    if (forceRedirect) {
        if (query.includes('#')) {
            window.location.href = `/profile/${encodeURIComponent(query)}`;
        } else {
            window.location.href = `/search?q=${encodeURIComponent(query)}`;
        }
        return;
    }

    try {
        const response = await fetch(`https://api.axioscomputers.com/api/v1/search/autocomplete?query=${encodeURIComponent(query)}`);

        if (response.ok) {
            const results = await response.json();
            renderResults(results);
        } else {
            renderResults([
                { name: query, type: 'Summoner', url: `/profile/${query}`, image: '' },
                { name: "Ahri", type: "Champion", url: "/champion/ahri", image: "https://ddragon.leagueoflegends.com/cdn/14.1.1/img/champion/Ahri.png" }
            ]);
        }
    } catch (error) {
        renderResults([
            { name: query, type: 'Summoner', url: `/profile/${query}`, image: '' },
            { name: "Ahri", type: "Champion", url: "/champion/ahri", image: "https://ddragon.leagueoflegends.com/cdn/14.1.1/img/champion/Ahri.png" }
        ]);
    }
}

function renderResults(results) {
    const dropdown = document.getElementById('searchDropdown');
    const searchContainer = document.querySelector('.nav-search');

    dropdown.innerHTML = '';
    currentSelectionIndex = -1;

    if (!results || results.length === 0) {
        closeDropdown();
        return;
    }

    results.forEach(item => {
        const div = document.createElement('div');
        div.className = 'search-item';
        div.onclick = () => window.location.href = item.url;

        div.innerHTML = `
            <img src="${item.image || 'assets/default-icon.png'}" alt="">
            <div class="info">
                <span class="main-text">${item.name}</span>
                <span class="sub-text">${item.type}</span>
            </div>
        `;
        dropdown.appendChild(div);
    });

    searchContainer.classList.add('open');
}

function updateSelection(items) {
    items.forEach(item => item.classList.remove('selected'));
    if (currentSelectionIndex > -1 && items[currentSelectionIndex]) {
        items[currentSelectionIndex].classList.add('selected');
    }
}

function closeDropdown() {
    const searchContainer = document.querySelector('.nav-search');
    const dropdown = document.getElementById('searchDropdown');

    if (searchContainer) searchContainer.classList.remove('open');
    if (dropdown) dropdown.innerHTML = '';
    currentSelectionIndex = -1;
}

window.addEventListener('DOMContentLoaded', () => {
    loadComponents();
    fetchUserProfile();
});