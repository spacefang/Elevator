/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{vue,js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                primary: '#409EFF',
                success: '#67C23A',
                warning: '#E6A23C', // 对应橙色告警
                danger: '#F56C6C',  // 对应红色告警
                info: '#909399',
                page: '#F5F7FA',    // 页面背景
                'content-bg': '#FFFFFF', // 内容背景
            }
        },
    },
    plugins: [],
}
