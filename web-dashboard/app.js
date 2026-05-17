// Supabase Configuration
const SUPABASE_URL = 'https://xamrcjcqrgixqdpxdrol.supabase.co';
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhhbXJjamNxcmdpeHFkcHhkcm9sIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg0MDA0NTIsImV4cCI6MjA5Mzk3NjQ1Mn0.7Wfu1j70Q-AFsNoonZVJLmb_WI23l3UgVNYuILVDTpk';

const sbClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// DOM Elements
const authOverlay = document.getElementById('auth-overlay');
const authSuccess = document.getElementById('auth-success');
const authFormContent = document.getElementById('auth-form-content');
const dashboardScreen = document.getElementById('dashboard-screen');
const authError = document.getElementById('auth-error');
const userEmailDisplay = document.getElementById('user-email');
const pageTitle = document.getElementById('page-title');

// Auth State
let currentUser = null;

// Initialize
async function init() {
    const { data: { session } } = await sbClient.auth.getSession();
    
    if (session) {
        currentUser = session.user;
        showDashboard(true); // Direct access if session exists
    } else {
        showAuth();
    }

    // Listen for auth changes
    sbClient.auth.onAuthStateChange((event, session) => {
        if (event === 'SIGNED_IN') {
            currentUser = session.user;
            showDashboard();
        } else if (event === 'SIGNED_OUT') {
            currentUser = null;
            showAuth();
        }
    });
}

// ── Auth Functions ──
async function handleSignIn() {
    console.log('Attempting Sign In...');
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
    if (!email || !password) {
        showError('Please enter email and password');
        return;
    }

    try {
        const { error } = await sbClient.auth.signInWithPassword({ email, password });
        if (error) {
            console.error('Sign In Error:', error.message);
            showError(error.message);
        } else {
            console.log('Sign In Success');
            showSuccessState();
        }
    } catch (err) {
        console.error('Unexpected Sign In Error:', err);
        showError('An unexpected error occurred.');
    }
}

async function handleSignUp() {
    console.log('Attempting Sign Up...');
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    
    if (!email || !password) {
        showError('Please enter email and password');
        return;
    }

    try {
        const { error } = await sbClient.auth.signUp({ email, password });
        if (error) {
            console.error('Sign Up Error:', error.message);
            showError(error.message);
        } else {
            console.log('Sign Up Success');
            showSuccessState();
        }
    } catch (err) {
        console.error('Unexpected Sign Up Error:', err);
        showError('An unexpected error occurred.');
    }
}

async function handleSignOut() {
    console.log('Signing Out...');
    await sbClient.auth.signOut();
}

function showError(msg, isError = true) {
    authError.textContent = msg;
    authError.style.backgroundColor = isError ? '#FEE2E2' : '#D1FAE5';
    authError.style.color = isError ? '#EF4444' : '#059669';
    authError.classList.remove('hidden');
}

function handleOAuthGoogle() {
    console.log('Google OAuth Sign In...');
    sbClient.auth.signInWithOAuth({
        provider: 'google',
    }).then(({ error }) => {
        if (error) {
            console.error('OAuth Error:', error.message);
            showError(error.message);
        }
    });
}

function handleGuestLogin() {
    console.log('Guest Login...');
    const fakeId = (window.crypto && window.crypto.randomUUID) 
        ? window.crypto.randomUUID() 
        : 'guest-' + Date.now();
    currentUser = { id: fakeId, email: 'guest@company.com' };
    showSuccessState();
}

function showSuccessState() {
    authFormContent.classList.add('hidden');
    authSuccess.classList.remove('hidden');
    
    // Smooth transition to dashboard after 1.5s (tick animation completes)
    setTimeout(() => {
        showDashboard();
    }, 1800);
}

// ── Map Entry Logic ──
let map, startMarker, endMarker;

function openMapModal() {
    if (!currentUser) {
        showError('Please sign in first');
        return;
    }
    document.getElementById('map-modal-overlay').classList.remove('hidden');
    
    if (!map) {
        map = L.map('map').setView([51.505, -0.09], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap'
        }).addTo(map);

        map.on('click', function(e) {
            if (!startMarker) {
                startMarker = L.marker(e.latlng).addTo(map);
                document.getElementById('map-start-text').value = `${e.latlng.lat.toFixed(4)}, ${e.latlng.lng.toFixed(4)}`;
            } else if (!endMarker) {
                endMarker = L.marker(e.latlng).addTo(map);
                document.getElementById('map-end-text').value = `${e.latlng.lat.toFixed(4)}, ${e.latlng.lng.toFixed(4)}`;
                calculateMapDistance();
            } else {
                map.removeLayer(startMarker);
                map.removeLayer(endMarker);
                startMarker = L.marker(e.latlng).addTo(map);
                endMarker = null;
                document.getElementById('map-start-text').value = `${e.latlng.lat.toFixed(4)}, ${e.latlng.lng.toFixed(4)}`;
                document.getElementById('map-end-text').value = '';
                document.getElementById('map-distance').textContent = '0.00';
            }
        });
    }
    
    setTimeout(() => map.invalidateSize(), 100);
}

function closeMapModal() {
    document.getElementById('map-modal-overlay').classList.add('hidden');
    if (startMarker) { map.removeLayer(startMarker); startMarker = null; }
    if (endMarker) { map.removeLayer(endMarker); endMarker = null; }
    document.getElementById('map-start-text').value = '';
    document.getElementById('map-end-text').value = '';
    document.getElementById('map-distance').textContent = '0.00';
}

function calculateMapDistance() {
    if (startMarker && endMarker) {
        const dist = map.distance(startMarker.getLatLng(), endMarker.getLatLng());
        document.getElementById('map-distance').textContent = (dist / 1000).toFixed(2);
    }
}

async function submitMapActivity() {
    if (!startMarker || !endMarker) {
        alert("Please set both Start and End points on the map.");
        return;
    }
    
    const distMeters = map.distance(startMarker.getLatLng(), endMarker.getLatLng());
    const mode = document.getElementById('map-transport-mode').value;
    
    const co2Saved = (distMeters / 1000) * 0.15;
    const points = Math.floor(distMeters / 100);

    const payload = {
        client_event_id: (window.crypto && window.crypto.randomUUID) ? window.crypto.randomUUID() : 'evt-' + Date.now(),
        user_id: currentUser.id,
        company_id: 'eco_enterprise_demo',
        transport_mode: mode,
        distance_meters: distMeters,
        co2_saved_kg: co2Saved,
        confidence_score: 1.0,
        points_awarded: points,
        recorded_at: new Date().toISOString(),
        source_client: 'flutter',
        start_label: document.getElementById('map-start-text').value,
        destination_label: document.getElementById('map-end-text').value
    };

    const { error } = await sbClient.from('esg_audit_logs').insert([payload]);
    
    if (error) {
        console.error('Submit Error:', error);
        alert('Submission failed: ' + error.message);
    } else {
        // Trigger local n8n directly since Cloud Supabase can't reach localhost
        try {
            await fetch('http://localhost:5678/webhook/supabase-esg-trigger', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ record: payload })
            });
        } catch (e) {
            console.warn('Local n8n trigger failed (CORS or offline):', e);
        }

        closeMapModal();
        loadDashboardData(); 
        alert('Custom Activity Logged! Discord AI webhook triggered. 🚛💨');
    }
}

// ── UI Navigation ──
function showAuth() {
    authOverlay.classList.remove('fade-out');
    authOverlay.classList.remove('hidden');
    authFormContent.classList.remove('hidden');
    authSuccess.classList.add('hidden');
    dashboardScreen.classList.add('hidden');
}

function showDashboard(isInstant = false) {
    if (isInstant) {
        authOverlay.classList.add('hidden');
        dashboardScreen.classList.remove('hidden');
    } else {
        authOverlay.classList.add('fade-out');
        setTimeout(() => {
            authOverlay.classList.add('hidden');
            dashboardScreen.classList.remove('hidden');
        }, 500);
    }
    
    if (currentUser) {
        userEmailDisplay.textContent = currentUser.email;
        loadDashboardData();
    }
}

function switchTab(tabId, element) {
    // Update nav styling
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    element.classList.add('active');

    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(el => el.classList.add('hidden'));
    
    // Show selected tab
    document.getElementById(`tab-${tabId}`).classList.remove('hidden');
    
    // Update title
    pageTitle.textContent = element.textContent.replace(/[^\w\s]/gi, '').trim();

    // Reload data if needed
    if (tabId === 'overview' || tabId === 'activities') {
        loadDashboardData();
    } else if (tabId === 'reports') {
        loadReports();
    } else if (tabId === 'research') {
        console.log('DAA Verification Tab Active');
    }
}

// ── Data Loading ──
async function loadDashboardData() {
    try {
        const { data: activities, error } = await sbClient
            .from('esg_audit_logs')
            .select('*')
            .order('recorded_at', { ascending: false });

        if (error) throw error;

        updateOverviewStats(activities);
        updateActivitiesTable(activities);
    } catch (err) {
        console.error('Error loading data:', err);
    }
}

async function generateReport() {
    if (!currentUser) return;
    try {
        const { data: activities, error: fetchErr } = await sbClient
            .from('esg_audit_logs')
            .select('*')
            .eq('user_id', currentUser.id);
            
        if (fetchErr) throw fetchErr;
        
        const totalCo2Saved = activities.reduce((sum, act) => sum + act.co2_saved_kg, 0);
        
        const payload = {
            id: (window.crypto && window.crypto.randomUUID) ? window.crypto.randomUUID() : 'rep-' + Date.now(),
            user_id: currentUser.id,
            companyid: 'eco_enterprise_demo',
            reportperiodstart: Date.now() - (30 * 24 * 60 * 60 * 1000),
            reportperiodend: Date.now(),
            totalco2kg: totalCo2Saved,
            offsetpurchased: 0,
            netco2kg: -totalCo2Saved,
            executivesummaryjson: JSON.stringify({ message: "Generated on-demand via Web Dashboard." }),
            pdfblobpath: null,
            createdat: Date.now(),
            islegallysubmitted: false
        };
        
        const { error } = await sbClient.from('sustainability_reports').insert([payload]);
        if (error) throw error;
        
        alert('Report Generated Successfully!');
        loadReports();
    } catch (err) {
        console.error('Error generating report:', err);
        alert('Failed to generate report: ' + err.message);
    }
}

async function loadReports() {
    try {
        const { data: reports, error } = await sbClient
            .from('sustainability_reports')
            .select('*')
            .order('createdat', { ascending: false });

        if (error) throw error;

        const container = document.getElementById('reports-list');
        
        if (!reports || reports.length === 0) {
            container.innerHTML = '<p class="empty-state">No audit-ready reports generated yet.</p>';
            return;
        }

        container.innerHTML = reports.map(r => `
            <div class="activity-row" style="margin-bottom: 1rem;">
                <div class="activity-left">
                    <div class="activity-icon">📋</div>
                    <div class="activity-details">
                        <h4>Sustainability Report</h4>
                        <p>Total CO₂: ${r.totalco2kg.toFixed(2)} kg | Net: ${r.netco2kg.toFixed(2)} kg</p>
                    </div>
                </div>
                <div style="display: flex; align-items: center; gap: 10px;">
                    <button class="btn btn-secondary" style="font-size: 0.8em; padding: 4px 8px; background: #5865F2; color: white; border: none;" onclick="sendReportToDiscord('${r.id}', ${r.totalco2kg}, ${r.netco2kg})">Discord Broadcast</button>
                    <span class="badge ${r.islegallysubmitted ? 'badge-verified' : 'badge-pending'}">
                        ${r.islegallysubmitted ? 'Verified' : 'Pending'}
                    </span>
                </div>
            </div>
        `).join('');
    } catch (err) {
        console.error('Error loading reports:', err);
    }
}

async function sendReportToDiscord(reportId, totalCo2, netCo2) {
    try {
        const payload = {
            transport_mode: '📊 OFFICIAL ESG REPORT',
            distance_meters: 0,
            co2_saved_kg: -netCo2, // because net is stored as negative
            points_awarded: 1000,
            client_event_id: reportId,
            source_client: 'dashboard_report',
            start_label: 'EcoTrack Auditing System',
            destination_label: 'Public Ledger',
            recorded_at: new Date().toISOString()
        };
        
        await fetch('http://localhost:5678/webhook/supabase-esg-trigger', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ record: payload })
        });
        
        alert('Report Broadcasted to Discord via n8n! Check your server.');
    } catch (e) {
        alert('Failed to send to n8n. Did you activate the workflow? Error: ' + e.message);
    }
}

// ── UI Updaters ──
function updateOverviewStats(activities) {
    if (!activities || activities.length === 0) return;

    // Total CO2 Saved
    const totalCo2 = activities.reduce((sum, act) => sum + act.co2_saved_kg, 0);
    document.getElementById('total-co2').textContent = totalCo2.toFixed(2);

    // Total Activities
    document.getElementById('total-activities').textContent = activities.length;

    // Total Points
    const totalPoints = activities.reduce((sum, act) => sum + act.points_awarded, 0);
    document.getElementById('total-points').textContent = totalPoints;

    // Mode Breakdown & Top Mode
    const modes = {};
    activities.forEach(act => {
        modes[act.transport_mode] = (modes[act.transport_mode] || 0) + 1;
    });

    let topMode = '—';
    let maxCount = 0;
    
    const breakdownHtml = Object.entries(modes).map(([mode, count]) => {
        if (count > maxCount) {
            maxCount = count;
            topMode = mode;
        }
        return `
            <div class="mode-item">
                <div class="mode-emoji">${getModeEmoji(mode)}</div>
                <div class="mode-count">${count}</div>
                <div class="mode-name">${mode.replace('_', ' ')}</div>
            </div>
        `;
    }).join('');

    document.getElementById('top-mode').textContent = topMode.replace('_', ' ');
    document.getElementById('mode-breakdown').innerHTML = breakdownHtml;

    // Recent Activities List
    const recentHtml = activities.slice(0, 5).map(act => `
        <div class="activity-row">
            <div class="activity-left">
                <div class="activity-icon">${getModeEmoji(act.transport_mode)}</div>
                <div class="activity-details">
                    <h4>${act.transport_mode.replace('_', ' ')}</h4>
                    <p>${new Date(act.recorded_at).toLocaleDateString()} · ${(act.distance_meters / 1000).toFixed(1)} km</p>
                </div>
            </div>
            <div class="activity-co2">+${act.co2_saved_kg.toFixed(3)} kg</div>
        </div>
    `).join('');
    
    document.getElementById('recent-activities').innerHTML = recentHtml;
}

function updateActivitiesTable(activities) {
    const tbody = document.getElementById('activities-table-body');
    
    if (!activities || activities.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No activities recorded.</td></tr>';
        return;
    }

    tbody.innerHTML = activities.map(act => `
        <tr>
            <td style="text-transform: capitalize;">${getModeEmoji(act.transport_mode)} ${act.transport_mode.replace('_', ' ')}</td>
            <td>${(act.distance_meters / 1000).toFixed(2)}</td>
            <td>${act.points_awarded}</td>
            <td style="font-weight: 600; color: #059669;">+${act.co2_saved_kg.toFixed(3)} kg</td>
            <td>${new Date(act.recorded_at).toLocaleDateString()}</td>
        </tr>
    `).join('');
}

// ── Helpers ──
function getModeEmoji(mode) {
    const map = {
        'IDLE': '🧍',
        'WALKING': '🚶',
        'TRANSIT': '🚌',
        'HEAVY_VEHICLE': '🚛'
    };
    return map[mode] || '📍';
}

function formatDuration(start, end) {
    const mins = Math.floor((end - start) / 60000);
    return `${mins} min`;
}

// ── Event Listeners ──
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('signin-btn')?.addEventListener('click', handleSignIn);
    document.getElementById('signup-btn')?.addEventListener('click', handleSignUp);
    document.getElementById('oauth-google-btn')?.addEventListener('click', handleOAuthGoogle);
    document.getElementById('guest-btn')?.addEventListener('click', handleGuestLogin);
    document.getElementById('simulate-btn')?.addEventListener('click', openMapModal);
    
    // Expose functions globally for HTML onclicks
    window.switchTab = switchTab;
    window.handleSignOut = handleSignOut;
    window.closeMapModal = closeMapModal;
    window.submitMapActivity = submitMapActivity;
    window.generateReport = generateReport;
    window.sendReportToDiscord = sendReportToDiscord;

    // Start app
    init();
});
