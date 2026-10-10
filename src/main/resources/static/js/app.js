/* Enhanced Interactive Frontend UX Engine for CareLink */
document.addEventListener('DOMContentLoaded', () => {
  // Section router for tabbed portals
  const q = new URLSearchParams(location.search);
  const section = ['dashboard', 'booking', 'history', 'profile'].includes(q.get('section')) ? q.get('section') : 'dashboard';
  
  document.querySelectorAll('.section-content').forEach(e => {
    e.classList.toggle('active', e.id === 'section-' + section);
  });
  
  document.querySelectorAll('[data-section]').forEach(e => {
    e.classList.toggle('active', e.dataset.section === section);
  });
  
  document.querySelectorAll('.sidebar nav a').forEach(e => {
    const u = new URL(e.href, location.origin);
    const active = location.pathname === u.pathname && (location.pathname !== '/patient/dashboard' || (u.searchParams.get('section') || 'dashboard') === section);
    e.classList.toggle('active', active);
    if (active) e.setAttribute('aria-current', 'page');
  });

  // Mobile sidebar drawer
  const mobileToggle = document.querySelector('.mobile-menu');
  const sidebar = document.getElementById('sidebar');
  if (mobileToggle && sidebar) {
    mobileToggle.addEventListener('click', () => {
      const open = sidebar.classList.toggle('open');
      mobileToggle.setAttribute('aria-expanded', String(open));
    });
    
    document.addEventListener('click', e => {
      if (sidebar.classList.contains('open') && !e.target.closest('.sidebar, .mobile-menu')) {
        sidebar.classList.remove('open');
        mobileToggle.setAttribute('aria-expanded', 'false');
      }
    });

    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && sidebar.classList.contains('open')) {
        sidebar.classList.remove('open');
        mobileToggle.setAttribute('aria-expanded', 'false');
      }
    });
  }

  // Double submission prevention & micro-loading
  document.querySelectorAll('form').forEach(form => {
    form.addEventListener('submit', () => {
      const submitBtn = form.querySelector('button[type="submit"], .btn:not(.secondary):not(.danger)');
      if (submitBtn && !submitBtn.disabled) {
        submitBtn.dataset.originalText = submitBtn.textContent;
        submitBtn.disabled = true;
        submitBtn.textContent = 'Processing...';
        setTimeout(() => {
          submitBtn.disabled = false;
          submitBtn.textContent = submitBtn.dataset.originalText;
        }, 10000);
      }
    });
  });

  // Safe Accessible Action Confirmation Dialog
  document.querySelectorAll('[data-confirm]').forEach(f => {
    f.addEventListener('submit', e => {
      const msg = f.dataset.confirm || 'Are you sure you want to proceed?';
      if (!confirm(msg)) {
        e.preventDefault();
        const submitBtn = f.querySelector('button[type="submit"], .btn');
        if (submitBtn) {
          submitBtn.disabled = false;
          if (submitBtn.dataset.originalText) submitBtn.textContent = submitBtn.dataset.originalText;
        }
      }
    });
  });

  // Fast live table filtering
  document.querySelectorAll('[data-filter]').forEach(input => {
    input.addEventListener('input', () => {
      const term = input.value.trim().toLowerCase();
      const targetTable = document.querySelector(input.dataset.filter);
      if (!targetTable) return;
      targetTable.querySelectorAll('tbody tr').forEach(row => {
        row.hidden = term.length > 0 && !row.textContent.toLowerCase().includes(term);
      });
    });
  });

  // Interactive Booking Stepper and Dynamic Slot Selector
  const form = document.getElementById('bookingForm');
  if (!form) return;

  const d = document.getElementById('selectedDoctorId');
  const date = document.getElementById('datePicker');
  const times = document.getElementById('timeSelect');
  const branch = document.getElementById('selectedBranchId');
  const area = document.getElementById('areaFilter');
  const specialty = document.getElementById('specializationFilter');
  const status = document.getElementById('slotStatus');
  const doctorStatus = document.getElementById('doctorStatus');

  const now = new Date();
  if (date) {
    date.min = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  }

  let requestId = 0;
  let doctorRequest = 0;
  let doctorOptions = [];

  const allBranches = branch ? Array.from(branch.options).filter(o => o.value).map(o => ({
    id: o.value,
    text: o.textContent,
    area: o.dataset.area
  })) : [];

  // Interactive Slot Chips Container
  const slotChipsContainer = document.getElementById('slotChipsContainer');

  function updateSlotChips(slots) {
    if (!slotChipsContainer) return;
    slotChipsContainer.innerHTML = '';
    if (!slots || slots.length === 0) return;

    const label = document.createElement('div');
    label.style.fontSize = '12px';
    label.style.fontWeight = '700';
    label.style.color = 'var(--ink-secondary)';
    label.style.marginBottom = '8px';
    label.style.textTransform = 'uppercase';
    label.style.letterSpacing = '0.05em';
    label.textContent = 'Instant Slot Selection:';
    slotChipsContainer.appendChild(label);

    const grid = document.createElement('div');
    grid.className = 'slot-chips-grid';

    slots.forEach(s => {
      const timeFormatted = new Date(s).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'slot-chip';
      chip.textContent = timeFormatted;
      if (times.value === s) chip.classList.add('selected');

      chip.addEventListener('click', () => {
        times.value = s;
        grid.querySelectorAll('.slot-chip').forEach(c => c.classList.remove('selected'));
        chip.classList.add('selected');
        updateReviewSummary();
      });

      grid.appendChild(chip);
    });

    slotChipsContainer.appendChild(grid);
  }

  function resetTimes() {
    requestId++;
    if (!times) return;
    times.replaceChildren(new Option('Choose doctor and date first', ''));
    times.disabled = true;
    if (slotChipsContainer) slotChipsContainer.innerHTML = '';
    if (status) status.textContent = 'Please choose a hospital, doctor and date to check slot availability.';
  }

  async function refresh() {
    resetTimes();
    const current = requestId;
    if (!branch?.value || !d?.value || !date?.value) return;
    if (status) status.textContent = 'Checking real-time clinic slot availability...';

    try {
      const apptId = form.querySelector('input[name="id"]')?.value;
      const params = new URLSearchParams({ date: date.value, branchId: branch.value });
      if (apptId) params.append('excludeAppointmentId', apptId);

      const response = await fetch(`/api/doctors/${encodeURIComponent(d.value)}/slots?${params}`, {
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error('Availability endpoint returned error');
      const slots = await response.json();
      if (current !== requestId) return;

      times.replaceChildren(new Option(slots.length ? 'Select a consultation time' : 'No available slots on this date', ''));
      slots.forEach(s => {
        const timeFormatted = new Date(s).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        times.add(new Option(timeFormatted, s));
      });
      times.disabled = slots.length === 0;
      updateSlotChips(slots);

      if (status) {
        status.textContent = slots.length 
          ? `${slots.length} available slots found at this hospital branch. Slot will be reserved on confirmation.`
          : 'No published clinic sessions for this doctor on the selected date. Please choose another date.';
      }
      updateReviewSummary();
    } catch (err) {
      if (current !== requestId) return;
      if (status) status.textContent = 'Could not load slot availability. Please try another date.';
      times.replaceChildren(new Option('Availability service unavailable', ''));
    }
  }

  function showDoctors(selected = '') {
    if (!d) return;
    const filtered = doctorOptions.filter(x => !specialty?.value || x.specialization === specialty.value);
    d.replaceChildren(new Option(filtered.length ? 'Select a medical specialist' : 'No specialists available', ''));
    filtered.forEach(x => {
      d.add(new Option(`${x.name} (${x.specialization || 'General'}) - LKR ${x.fee}`, x.id));
    });
    if (selected && filtered.some(x => String(x.id) === String(selected))) {
      d.value = selected;
    }
    d.disabled = filtered.length === 0;
    refresh();
    updateReviewSummary();
  }

  async function loadDoctors(selected = '') {
    const current = ++doctorRequest;
    resetTimes();
    doctorOptions = [];
    if (!d) return;
    d.replaceChildren(new Option('Loading doctors for branch...', ''));
    d.disabled = true;

    if (specialty) specialty.replaceChildren(new Option('All medical specializations', ''));

    if (!branch?.value) {
      d.replaceChildren(new Option('Choose a hospital branch first', ''));
      if (doctorStatus) doctorStatus.textContent = 'Select a hospital branch above to view active specialists.';
      return;
    }

    try {
      const response = await fetch(`/api/branches/${encodeURIComponent(branch.value)}/doctors`, {
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error();
      const data = await response.json();
      if (current !== doctorRequest) return;
      doctorOptions = data;

      if (specialty) {
        [...new Set(data.map(x => x.specialization).filter(Boolean))].sort().forEach(sp => {
          specialty.add(new Option(sp, sp));
        });
      }

      if (doctorStatus) {
        doctorStatus.textContent = data.length 
          ? `${data.length} registered specialists available at this hospital branch.`
          : 'No active specialists registered with published clinic sessions at this location.';
      }
      showDoctors(selected);
    } catch (e) {
      if (current !== doctorRequest) return;
      d.replaceChildren(new Option('Specialists currently unavailable', ''));
      if (doctorStatus) doctorStatus.textContent = 'Could not connect to hospital directory. Please select again.';
    }
  }

  if (area && branch) {
    area.addEventListener('input', () => {
      const selected = branch.value;
      const term = area.value.trim().toLowerCase();
      branch.replaceChildren(new Option('Choose a hospital branch', ''));
      allBranches
        .filter(x => (x.area + ' ' + x.text).toLowerCase().includes(term))
        .forEach(x => branch.add(new Option(x.text, x.id)));
      branch.value = selected;
      if (branch.value !== selected) loadDoctors();
      updateReviewSummary();
    });
  }

  branch?.addEventListener('change', () => { loadDoctors(); updateReviewSummary(); });
  specialty?.addEventListener('change', () => { showDoctors(); updateReviewSummary(); });
  d?.addEventListener('change', () => { refresh(); updateReviewSummary(); });
  date?.addEventListener('change', () => { refresh(); updateReviewSummary(); });
  times?.addEventListener('change', () => {
    updateReviewSummary();
    if (slotChipsContainer) {
      slotChipsContainer.querySelectorAll('.slot-chip').forEach(c => {
        c.classList.toggle('selected', times.options[times.selectedIndex]?.text.trim() === c.textContent.trim());
      });
    }
  });

  // 4-Stage Directional Wizard Transitions
  let currentStep = 1;
  const wizardTitles = {
    1: { title: 'Stage 1: Choose Hospital Branch', desc: 'Filter location and select an accredited hospital facility.' },
    2: { title: 'Stage 2: Select Specialist & Doctor', desc: 'Choose clinical department and consultation physician.' },
    3: { title: 'Stage 3: Date & Consultation Time Slot', desc: 'Pick an upcoming date and verify real-time available clinic slot.' },
    4: { title: 'Stage 4: Review Booking & Contact Details', desc: 'Confirm appointment details and provide reminder contacts.' }
  };

  function updateReviewSummary() {
    const revBranch = document.getElementById('reviewBranch');
    const revDoc = document.getElementById('reviewDoctor');
    const revDate = document.getElementById('reviewDate');
    const revTime = document.getElementById('reviewTime');

    if (revBranch && branch) {
      revBranch.textContent = branch.selectedIndex > 0 ? branch.options[branch.selectedIndex].text : 'Not selected';
    }
    if (revDoc && d) {
      revDoc.textContent = d.selectedIndex > 0 ? d.options[d.selectedIndex].text : 'Not selected';
    }
    if (revDate && date) {
      revDate.textContent = date.value ? date.value : 'Not selected';
    }
    if (revTime && times) {
      revTime.textContent = times.selectedIndex > 0 ? times.options[times.selectedIndex].text : 'Not selected';
    }
  }

  function goToStep(target) {
    if (target < 1 || target > 4) return;
    const isForward = target > currentStep;
    const animClass = isForward ? 'slide-next' : 'slide-prev';

    // Validation when advancing
    if (isForward) {
      if (currentStep === 1 && (!branch || !branch.value)) {
        alert('Please choose a hospital branch before continuing.');
        branch?.focus();
        return;
      }
      if (currentStep === 2 && (!d || !d.value)) {
        alert('Please choose a medical specialist before continuing.');
        d?.focus();
        return;
      }
      if (currentStep === 3) {
        if (!date || !date.value) {
          alert('Please select a consultation date.');
          date?.focus();
          return;
        }
        if (!times || !times.value) {
          alert('Please select an available consultation slot.');
          times?.focus();
          return;
        }
      }
    }

    currentStep = target;

    // Panes
    document.querySelectorAll('.step-pane').forEach(pane => {
      const pNum = Number(pane.dataset.pane);
      if (pNum === currentStep) {
        pane.classList.remove('slide-next', 'slide-prev');
        void pane.offsetWidth; // Force reflow for animation restart
        pane.classList.add('active', animClass);
      } else {
        pane.classList.remove('active', 'slide-next', 'slide-prev');
      }
    });

    // Stepper header items
    document.querySelectorAll('.step-item').forEach(item => {
      const sNum = Number(item.dataset.step);
      item.classList.toggle('active', sNum === currentStep);
      item.classList.toggle('completed', sNum < currentStep);
    });

    // Dividers
    document.querySelectorAll('.step-divider').forEach(div => {
      const dNum = Number(div.dataset.divider);
      div.classList.toggle('active', dNum < currentStep);
    });

    // Update titles
    const titleEl = document.getElementById('wizardStepTitle');
    const descEl = document.getElementById('wizardStepDesc');
    if (titleEl && wizardTitles[currentStep]) titleEl.textContent = wizardTitles[currentStep].title;
    if (descEl && wizardTitles[currentStep]) descEl.textContent = wizardTitles[currentStep].desc;

    updateReviewSummary();
    const cardEl = form.closest('.card');
    if (cardEl) {
      cardEl.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  document.querySelectorAll('.wizard-next-btn').forEach(btn => {
    btn.addEventListener('click', () => goToStep(Number(btn.dataset.target)));
  });

  document.querySelectorAll('.wizard-prev-btn').forEach(btn => {
    btn.addEventListener('click', () => goToStep(Number(btn.dataset.target)));
  });

  document.querySelectorAll('.step-item').forEach(item => {
    item.addEventListener('click', () => {
      const target = Number(item.dataset.step);
      if (target <= currentStep || (target === currentStep + 1)) {
        goToStep(target);
      }
    });
  });

  const bookMyself = document.getElementById('bookMyself');
  const bookOther = document.getElementById('bookOther');
  const patientNameInput = document.getElementById('inputPatientName');
  let myDefaultName = patientNameInput ? patientNameInput.value : '';

  if (bookMyself && bookOther && patientNameInput) {
    bookOther.addEventListener('change', () => {
      if (bookOther.checked) {
        patientNameInput.value = '';
        patientNameInput.placeholder = 'Enter family member full name';
        patientNameInput.focus();
      }
    });
    bookMyself.addEventListener('change', () => {
      if (bookMyself.checked) {
        patientNameInput.value = myDefaultName;
        patientNameInput.placeholder = 'e.g. Nimali Perera';
      }
    });
  }

  form.addEventListener('submit', e => {
    if (times?.disabled || !times?.value || !branch?.value || d?.disabled) {
      e.preventDefault();
      if (status) status.textContent = 'Please complete all booking selections (Hospital, Doctor, Date, and Time Slot).';
      alert('Please complete all booking selections before confirming.');
      goToStep(1);
    }
  });

  if (d?.dataset.selected) {
    loadDoctors(d.dataset.selected);
  } else if (branch?.value) {
    loadDoctors();
  }
});
