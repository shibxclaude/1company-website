// 向下滚动tab变灰
window.addEventListener('scroll', function () {
	var tabBar = document.querySelector('.tab-bar');
	var scrollPosition = window.pageYOffset;
	if (scrollPosition > 500) {
		tabBar.style.backgroundColor = 'rgba(128, 128, 128, 0.5)';
	} else {
		tabBar.style.backgroundColor = 'transparent';
	}
});
// 下拉菜单
window.onload = function () {
    const tabs = document.querySelectorAll('.tab');
    const dropdowns = document.querySelectorAll('.dropdown');
    function hideDropdowns() {
        dropdowns.forEach(dropdown => dropdown.style.display = 'none');
    }
    tabs.forEach(tab => {
        const dropdownId = tab.id + '-dropdown';
        const dropdown = document.getElementById(dropdownId);
        tab.addEventListener('mouseenter', () => {
            hideDropdowns();
            dropdown.style.display = 'block';
        });
        tab.addEventListener('mouseleave', (event) => {
            setTimeout(() => {
                if (!dropdown.matches(':hover')) {
                    dropdown.style.display = 'none';
                }
            }, 100);
        });
        dropdown.addEventListener('mouseleave', (event) => {
            setTimeout(() => {
                if (!dropdown.matches(':hover') &&!tab.matches(':hover')) {
                    dropdown.style.display = 'none';
                }
            }, 100);
        });
    });
}