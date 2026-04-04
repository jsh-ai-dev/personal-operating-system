(function () {
    var STORAGE_KEY = "pos-theme";
    var DARK = "dark";
    var LIGHT = "light";

    function applyTheme(theme) {
        var normalized = theme === DARK ? DARK : LIGHT;
        document.documentElement.setAttribute("data-theme", normalized);
        return normalized;
    }

    function updateToggleLabel(toggle, theme) {
        if (!toggle) {
            return;
        }
        toggle.textContent = theme === DARK ? "라이트모드" : "다크모드";
    }

    document.addEventListener("DOMContentLoaded", function () {
        var savedTheme = localStorage.getItem(STORAGE_KEY);
        var activeTheme = applyTheme(savedTheme);
        var toggle = document.getElementById("themeToggle");

        updateToggleLabel(toggle, activeTheme);
        if (!toggle) {
            return;
        }

        toggle.addEventListener("click", function () {
            var currentTheme = document.documentElement.getAttribute("data-theme") === DARK ? DARK : LIGHT;
            var nextTheme = currentTheme === DARK ? LIGHT : DARK;
            var applied = applyTheme(nextTheme);
            localStorage.setItem(STORAGE_KEY, applied);
            updateToggleLabel(toggle, applied);
        });
    });
})();

