// Vue 3 Demo App
(function() {
    console.log('[Vue Demo] Initializing app...');
    
    if (typeof Vue === 'undefined') {
        console.error('[Vue Demo] Vue is not defined!');
        return;
    }
    
    const App = {
        setup() {
            const count = Vue.ref(0);
            const increment = () => count.value++;
            const decrement = () => count.value--;
            
            return { count, increment, decrement };
        },
        template: `
            <div>
                <svg class="logo" viewBox="0 0 128 128">
                    <path fill="#42b883" d="M78.8,10L64,35.4L49.2,10H0l64,110l64-110H78.8z"/>
                    <path fill="#35495e" d="M78.8,10L64,35.4L49.2,10H25.6L64,76l38.4-66H78.8z"/>
                </svg>
                <h1>تطبيق Vue</h1>
                <p>تطبيق Vue 3 تجريبي يعرض وظيفة العداد التفاعلي.</p>
                
                <div class="counter">
                    <button @click="decrement">−</button>
                    <span>{{ count }}</span>
                    <button @click="increment">+</button>
                </div>
                
                <div class="features">
                    <div class="feature">
                        <div class="feature-icon">⚡</div>
                        تفاعلي
                    </div>
                    <div class="feature">
                        <div class="feature-icon">🎯</div>
                        مكونات
                    </div>
                    <div class="feature">
                        <div class="feature-icon">📦</div>
                        خفيف الوزن
                    </div>
                    <div class="feature">
                        <div class="feature-icon">🔧</div>
                        قابل للتوسيع
                    </div>
                </div>
            </div>
        `
    };
    
    try {
        Vue.createApp(App).mount('#app');
        console.log('[Vue Demo] App mounted successfully!');
    } catch (e) {
        console.error('[Vue Demo] Failed to mount app:', e);
    }
})();
