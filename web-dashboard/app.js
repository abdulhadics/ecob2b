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

function showSuccessState() {
    authFormContent.classList.add('hidden');
    authSuccess.classList.remove('hidden');
    
    // Smooth transition to dashboard after 1.5s (tick animation completes)
    setTimeout(() => {
        showDashboard();
    }, 1800);
}

async function simulateFleetActivity() {
    if (!currentUser) {
        showError('Please sign in first');
        return;
    }
    
    console.log('Simulating Fleet Activity...');
    const mockActivity = {
        id: crypto.randomUUID(),
        transportmode: 'TRUCK_HEAVY_VEHICLE',
        starttimestampms: Date.now() - 3600000,
        endtimestampms: Date.now(),
        distancemeters: 15400.5,
        avgspeedmps: 12.5,
        co2kgemitted: 4.85,
        companyid: 'eco_enterprise_demo',
        isauditverified: true,
        user_id: currentUser.id
    };

    const { error } = await sbClient.from('transport_activities').insert([mockActivity]);
    
    if (error) {
        console.error('Simulation Error:', error);
        showError('Simulation failed: ' + error.message);
    } else {
        console.log('Simulation Success');
        loadDashboardData(); // Refresh UI
        showError('Mock Activity Generated! n8n triggered. 🚛💨', false);
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
    }
}

// ── Data Loading ──
async function loadDashboardData() {
    try {
        const { data: activities, error } = await sbClient
            .from('transport_activities')
            .select('*')
            .order('starttimestampms', { ascending: false });

        if (error) throw error;

        updateOverviewStats(activities);
        updateActivitiesTable(activities);
    } catch (err) {
        console.error('Error loading data:', err);
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
                <div>
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

// ── UI Updaters ──
function updateOverviewStats(activities) {
    if (!activities || activities.length === 0) return;

    // Total CO2
    const totalCo2 = activities.reduce((sum, act) => sum + act.co2kgemitted, 0);
    document.getElementById('total-co2').textContent = totalCo2.toFixed(2);

    // Total Activities
    document.getElementById('total-activities').textContent = activities.length;

    // Average Speed
    const avgSpeed = activities.reduce((sum, act) => sum + act.avgspeedmps, 0) / activities.length;
    document.getElementById('avg-speed').textContent = avgSpeed.toFixed(1);

    // Mode Breakdown & Top Mode
    const modes = {};
    activities.forEach(act => {
        modes[act.transportmode] = (modes[act.transportmode] || 0) + 1;
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
                <div class="activity-icon">${getModeEmoji(act.transportmode)}</div>
                <div class="activity-details">
                    <h4>${act.transportmode.replace('_', ' ')}</h4>
                    <p>${formatDuration(act.starttimestampms, act.endtimestampms)} · ${act.avgspeedmps.toFixed(1)} m/s</p>
                </div>
            </div>
            <div class="activity-co2">${act.co2kgemitted.toFixed(3)} kg</div>
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
            <td style="text-transform: capitalize;">${getModeEmoji(act.transportmode)} ${act.transportmode.replace('_', ' ')}</td>
            <td>${formatDuration(act.starttimestampms, act.endtimestampms)}</td>
            <td>${act.avgspeedmps.toFixed(1)} m/s</td>
            <td style="font-weight: 600;">${act.co2kgemitted.toFixed(3)} kg</td>
            <td>
                <span class="badge ${act.isauditverified ? 'badge-verified' : 'badge-pending'}">
                    ${act.isauditverified ? 'Yes' : 'No'}
                </span>
            </td>
        </tr>
    `).join('');
}

// ── Helpers ──
function getModeEmoji(mode) {
    const map = {
        'STATIONARY': '🧍',
        'WALKING': '🚶',
        'CYCLING': '🚲',
        'PUBLIC_TRANSIT': '🚌',
        'CAR': '🚗',
        'TRUCK_HEAVY_VEHICLE': '🚛'
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
    document.getElementById('simulate-btn')?.addEventListener('click', simulateFleetActivity);
    
    // Expose switchTab globally since it's used in sidebar nav
    window.switchTab = switchTab;
    window.handleSignOut = handleSignOut;

    // Start app
    init();
});
