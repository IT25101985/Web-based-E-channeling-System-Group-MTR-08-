/* Hospital map searches use public branch addresses; no patient data is sent. */
document.addEventListener('DOMContentLoaded', () => {
  const select = document.getElementById('hospitalMapSelect');
  if (!select) return;
  const frame = document.getElementById('hospitalMapFrame');
  const name = document.getElementById('hospitalMapName');
  const address = document.getElementById('hospitalMapAddress');
  const actions = document.getElementById('hospitalMapActions');
  const directions = document.getElementById('hospitalMapDirections');
  const external = document.getElementById('hospitalMapExternal');
  function showBranch() {
    const option = select.selectedOptions[0];
    if (!option || !option.value) {
      name.textContent = 'Our hospital network';
      address.textContent = 'Choose a hospital from the list or use View on map below.';
      actions.hidden = true;
      directions.removeAttribute('href');
      external.removeAttribute('href');
      frame.title = 'Hospital locations in Sri Lanka';
      frame.src = 'https://maps.google.com/maps?q=Sri%20Lanka&z=7&output=embed';
      return;
    }
    const location = [option.dataset.address, option.dataset.area, 'Sri Lanka']
      .filter(value => value && value.trim()).join(', ');
    const query = encodeURIComponent(location);
    name.textContent = option.dataset.name;
    address.textContent = location;
    frame.title = 'Address map for ' + option.dataset.name;
    frame.src = 'https://maps.google.com/maps?q=' + query + '&z=15&output=embed';
    directions.href = 'https://www.google.com/maps/dir/?api=1&destination=' + query;
    external.href = 'https://www.google.com/maps/search/?api=1&query=' + query;
    actions.hidden = false;
  }
  select.addEventListener('change', showBranch);
  document.querySelectorAll('[data-show-hospital-map]').forEach(button => {
    button.addEventListener('click', () => {
      select.value = button.dataset.showHospitalMap;
      showBranch();
      document.getElementById('hospital-map').scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
        block: 'center'
      });
      select.focus({preventScroll: true});
    });
  });
});
