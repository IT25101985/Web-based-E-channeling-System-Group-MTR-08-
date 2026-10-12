/**
 * MediCare Pro — Sri Lanka National E-Channeling Platform
 * Trilingual Localization, Hero Search, Quick Chips, Modal & Responsive Interactivity
 */
document.addEventListener('DOMContentLoaded', () => {

  // =========================================================================
  // 1. TRILINGUAL LOCALIZATION ENGINE (English, Sinhala, Tamil)
  // =========================================================================
  const i18nDictionary = {
    en: {
      ribbon_text: "SLMC Accredited Specialists • Islandwide Hospitals • Counter Cash Payments",
      ribbon_hotline: "Hotline: 1990 (24/7)",
      nav_home: "Home",
      nav_find_doctors: "Find Doctors",
      nav_specializations: "Specializations",
      nav_available: "Available Today",
      nav_hospitals: "Hospitals",
      nav_how_it_works: "How It Works",
      nav_why_us: "Why Choose Us",
      nav_faqs: "FAQs",
      nav_sign_in: "Sign In",
      nav_book_cta: "Book Appointment",
      nav_more: "More",
      nav_about: "About & Contact",
      hero_badge: "Sri Lanka's Digital Healthcare Platform",
      hero_title: "Your Health, Our Priority",
      hero_sub: "Find trusted specialists and book verified clinic appointments across Sri Lanka in under 60 seconds.",
      label_doctor: "Doctor Name",
      label_specialty: "Specialization",
      label_hospital: "Hospital",
      opt_all_specializations: "All Specializations",
      opt_all_hospitals: "All Hospitals",
      btn_search_doctors: "Search Doctors",
      quick_chips_label: "Quick Search:",
      tick_slmc: "SLMC Certified Doctors",
      tick_instant: "Instant Slot Reservation",
      tick_fees: "Transparent Fees",
      eyebrow_specialties: "CLINICAL DEPARTMENTS",
      title_specialties: "Popular Specializations",
      sub_specialties: "Consult verified consultants specialized in key health domains.",
      eyebrow_available: "INSTANT CHANNELING",
      title_available: "Doctors & Today's Availability",
      sub_available: "Inspect verified consultants with published clinic sessions and reserve your slot.",
      eyebrow_hospitals: "ISLANDWIDE NETWORK",
      title_hospitals: "Top Accredited Hospitals",
      sub_hospitals: "Consultations take place at certified partner hospital branches across Sri Lanka.",
      eyebrow_how: "QUICK 60-SECOND PROCESS",
      title_how: "How It Works",
      sub_how: "Book your doctor consultation in three straightforward, transparent steps.",
      step1_title: "1. Search Doctor",
      step1_desc: "Search by medical specialty, doctor name, or hospital location across Sri Lanka.",
      step2_title: "2. Pick a Slot",
      step2_desc: "Choose your convenient date and select an available 1-hour clinic time slot.",
      step3_title: "3. Confirm & Attend",
      step3_desc: "Get your instant reference number and pay transparently via cash at the hospital counter.",
      eyebrow_why: "PLATFORM ADVANTAGES",
      title_why: "Why Choose MediCare Pro",
      sub_why: "Engineered for absolute reliability, patient trust, and hospital transparency.",
      b1_title: "Zero Double Booking",
      b1_desc: "Hardware-level database locking ensures your selected time slot is instantly reserved and protected.",
      b2_title: "Transparent Counter Cash",
      b2_desc: "No hidden platform markups. Settle consultation fees directly at the hospital reception counter.",
      b3_title: "Portal Notifications",
      b3_desc: "View confirmations in your portal. Email requires hospital configuration; SMS is not enabled.",
      b4_title: "SLMC Verified Doctors",
      b4_desc: "100% of medical consultants are credentialed by the Sri Lanka Medical Council.",
      eyebrow_testi: "PATIENT STORIES",
      title_testi: "Trusted by Sri Lankan Families",
      sub_testi: "Read authentic feedback from patients who booked specialists through MediCare Pro.",
      eyebrow_faqs: "COMMON QUESTIONS",
      title_faqs: "Frequently Asked Questions",
      sub_faqs: "Clear answers about booking, payments, cancellation, and hospital visits.",
      cta_title: "Ready to Book Your Doctor Consultation?",
      cta_desc: "Join thousands of Sri Lankan patients who reserve verified hospital appointments with zero waiting time.",
      cta_btn_book: "Book Appointment Now →",
      cta_btn_portal: "Sign In to Patient Portal",
      mob_search: "Search Doctors",
      mob_call: "Hotline 1990"
    },
    si: {
      ribbon_text: "SLMC අනුමත විශේෂඥ වෛද්‍යවරුන් • දිවයින පුරා රෝහල් • කවුන්ටර මුදල් ගෙවීම්",
      ribbon_hotline: "ක්ෂණික ඇමතුම්: 1990 (24/7)",
      nav_home: "මුල් පිටුව",
      nav_find_doctors: "වෛද්‍යවරුන් සොයන්න",
      nav_specializations: "විශේෂඥ ක්ෂේත්‍ර",
      nav_available: "අද දින ලබාගත හැකි",
      nav_hospitals: "රෝහල්",
      nav_how_it_works: "ක්‍රියාකාරීත්වය",
      nav_why_us: "අප තෝරාගත යුත්තේ ඇයි",
      nav_faqs: "නිතර අසන ප්‍රශ්න",
      nav_sign_in: "ඇතුල් වන්න",
      nav_book_cta: "වෙන්කරවා ගැනීම",
      nav_more: "තවත්",
      nav_about: "අපි ගැන & සම්බන්ධතා",
      hero_badge: "ශ්‍රී ලංකාවේ ඩිජිටල් සෞඛ්‍ය වේදිකාව",
      hero_title: "ඔබගේ සෞඛ්‍යය, අපගේ ප්‍රමුඛතාවයයි",
      hero_sub: "තත්පර 60ක් ඇතුළත ශ්‍රී ලංකාවේ විශ්වාසදායී වෛද්‍යවරුන් සොයා පහසුවෙන්ම දිනයක් වෙන්කරවා ගන්න.",
      label_doctor: "වෛද්‍යවරයාගේ නම",
      label_specialty: "විශේෂඥ ක්ෂේත්‍රය",
      label_hospital: "රෝහල",
      opt_all_specializations: "සියලුම විශේෂඥ ක්ෂේත්‍ර",
      opt_all_hospitals: "සියලුම රෝහල්",
      btn_search_doctors: "වෛද්‍යවරුන් සොයන්න",
      quick_chips_label: "ඉක්මන් සෙවුම:",
      tick_slmc: "SLMC සහතිකලත් වෛද්‍යවරුන්",
      tick_instant: "ක්ෂණික වේලාවන් වෙන්කිරීම",
      tick_fees: "විනිවිද පෙනෙන ගාස්තු",
      eyebrow_specialties: "වෛද්‍ය අංශ",
      title_specialties: "ජනප්‍රිය විශේෂඥ ක්ෂේත්‍ර",
      sub_specialties: "ප්‍රධාන සෞඛ්‍ය අංශවල පිළිගත් විශේෂඥ වෛද්‍යවරුන් හමුවන්න.",
      eyebrow_available: "ක්ෂණික හමුවීම්",
      title_available: "අද දින හමුවීම් • ක්ෂණික වෙන්කිරීම්",
      sub_available: "ප්‍රකාශිත සායන වේලාවන් පරීක්ෂා කර ඔබගේ වාරය වෙන්කරගන්න.",
      eyebrow_hospitals: "රෝහල් ජාලය",
      title_hospitals: "අනුමත ප්‍රමුඛ රෝහල්",
      sub_hospitals: "ශ්‍රී ලංකාව පුරා පිහිටි පිළිගත් හවුල්කාර රෝහල්වලදී වෛද්‍යවරයා හමුවන්න.",
      eyebrow_how: "තත්පර 60ක පහසු පියවර",
      title_how: "ක්‍රියාකාරීත්වය",
      sub_how: "පැහැදිලි පියවර තුනකින් ඔබගේ වෛද්‍ය හමුවීම වෙන්කරගන්න.",
      step1_title: "1. වෛද්‍යවරයා සොයන්න",
      step1_desc: "විශේෂඥ ක්ෂේත්‍රය, නම හෝ රෝහල අනුව සොයන්න.",
      step2_title: "2. වේලාව තෝරන්න",
      step2_desc: "ඔබට පහසු දිනය සහ පැයක සායන වේලාව තෝරන්න.",
      step3_title: "3. තහවුරු කර පැමිණෙන්න",
      step3_desc: "යොමු අංකය ලබාගෙන රෝහල් කවුන්ටරයෙන් මුදල් ගෙවන්න.",
      eyebrow_why: "අපගේ විශේෂත්වය",
      title_why: "MediCare Pro තෝරාගත යුත්තේ ඇයි",
      sub_why: "පූර්ණ විශ්වසනීයත්වය සහ රෝහල් විනිවිදභාවය වෙනුවෙන් නිර්මාණය කර ඇත.",
      b1_title: "ද්විත්ව වෙන්කිරීම් නැත",
      b1_desc: "දත්ත පද්ධතිය මගින් ඔබගේ වේලාව ක්ෂණිකව සුරක්ෂිත කෙරේ.",
      b2_title: "කවුන්ටර මුදල් ගෙවීම්",
      b2_desc: "අමතර අයකිරීම් නැත. රෝහල් පිළිගැනීමේ කවුන්ටරයෙන් සෘජුවම ගෙවන්න.",
      b3_title: "ද්වාර දැනුම්දීම්",
      b3_desc: "වෙන්කිරීම් තහවුරු කිරීම් ද්වාරයෙන් බලන්න. ඊමේල් සඳහා සේවාව සකස් කළ යුතුය. SMS සක්‍රිය නැත.",
      b4_title: "SLMC සහතිකලත් වෛද්‍යවරුන්",
      b4_desc: "සියලුම වෛද්‍යවරුන් ශ්‍රී ලංකා වෛද්‍ය සභාවේ ලියාපදිංචි වී ඇත.",
      eyebrow_testi: "රෝගීන්ගේ අදහස්",
      title_testi: "ශ්‍රී ලාංකික පවුල්වල විශ්වාසය",
      sub_testi: "MediCare Pro භාවිතා කළ රෝගීන්ගේ සත්‍ය අදහස්.",
      eyebrow_faqs: "පොදු ප්‍රශ්න",
      title_faqs: "නිතර අසන ප්‍රශ්න",
      sub_faqs: "ගෙවීම්, අවලංගු කිරීම් සහ රෝහල් පැමිණීම් පිළිබඳ පැහැදිලි තොරතුරු.",
      cta_title: "වෛද්‍යවරයකු හමුවීමට සූදානම්ද?",
      cta_desc: "පෝලිම්වල රැඳීසිටීමෙන් තොරව ක්ෂණිකව වෛද්‍යවරයකු වෙන්කරවා ගන්න.",
      cta_btn_book: "දැන්ම වෙන්කරවා ගන්න →",
      cta_btn_portal: "රෝගී ගිණුමට පිවිසෙන්න",
      mob_search: "වෛද්‍යවරුන් සොයන්න",
      mob_call: "ක්ෂණික ඇමතුම් 1990"
    },
    ta: {
      ribbon_text: "SLMC அங்கீகரிக்கப்பட்ட நிபுணர்கள் • நாடளாவிய மருத்துவமனைகள் • கவுண்டர் பணக் கொடுப்பனவு",
      ribbon_hotline: "அவசர உதவி: 1990 (24/7)",
      nav_home: "முகப்பு",
      nav_find_doctors: "மருத்துவர்களைத் தேடுங்கள்",
      nav_specializations: "துறைகள்",
      nav_available: "இன்று கிடைக்கும்",
      nav_hospitals: "மருத்துவமனைகள்",
      nav_how_it_works: "செயல்முறை",
      nav_why_us: "எங்களை ஏன் தெரிவு செய்ய வேண்டும்",
      nav_faqs: "கேள்வி-பதில்",
      nav_sign_in: "உள்நுழைக",
      nav_book_cta: "முன்பதிவு செய்க",
      nav_more: "மேலும்",
      nav_about: "எங்களைப் பற்றி & தொடர்பு",
      hero_badge: "இலங்கையின் டிஜிட்டல் சுகாதார தளம்",
      hero_title: "உங்கள் ஆரோக்கியம், எங்கள் முன்னுரிமை",
      hero_sub: "60 வினாடிகளுக்குள் இலங்கையின் நம்பகமான மருத்துவ நிபுணர்களைக் கண்டறிந்து முன்பதிவு செய்யுங்கள்.",
      label_doctor: "மருத்துவர் பெயர்",
      label_specialty: "சிறப்புத் துறை",
      label_hospital: "மருத்துவமனை",
      opt_all_specializations: "அனைத்து சிறப்புத் துறைகளும்",
      opt_all_hospitals: "அனைத்து மருத்துவமனைகளும்",
      btn_search_doctors: "மருத்துவர்களைத் தேடுங்கள்",
      quick_chips_label: "விரைவுத் தேடல்:",
      tick_slmc: "SLMC சான்றளிக்கப்பட்ட மருத்துவர்கள்",
      tick_instant: "உடனடி நேர முன்பதிவு",
      tick_fees: "வெளிப்படையான கட்டணங்கள்",
      eyebrow_specialties: "மருத்துவப் பிரிவுகள்",
      title_specialties: "பிரபலமான சிறப்புத் துறைகள்",
      sub_specialties: "முக்கிய சுகாதாரத் துறைகளில் அங்கீகரிக்கப்பட்ட நிபுணர்களை அணுகுங்கள்.",
      eyebrow_available: "உடனடி சேவைகள்",
      title_available: "இன்று கிடைக்கும் மருத்துவர்கள்",
      sub_available: "வெளியிடப்பட்ட மருத்துவ நேரங்களை சரிபார்த்து உடனே முன்பதிவு செய்யுங்கள்.",
      eyebrow_hospitals: "மருத்துவமனை நெட்வொர்க்",
      title_hospitals: "முன்னணி அங்கீகரிக்கப்பட்ட மருத்துவமனைகள்",
      sub_hospitals: "நாடு முழுவதும் உள்ள சான்றளிக்கப்பட்ட மருத்துவமனைகளில் சந்திப்புகள் நடைபெறும்.",
      eyebrow_how: "60 வினாடி செயல்முறை",
      title_how: "எவ்வாறு செயல்படுகிறது",
      sub_how: "மூன்று எளிய மற்றும் வெளிப்படையான படிகளில் உங்கள் முன்பதிவை நிறைவு செய்யுங்கள்.",
      step1_title: "1. மருத்துவரைத் தேடுங்கள்",
      step1_desc: "துறை, மருத்துவர் பெயர் அல்லது மருத்துவமனை வாரியாகத் தேடுங்கள்.",
      step2_title: "2. நேரத்தைத் தேர்ந்தெடுங்கள்",
      step2_desc: "உங்களுக்கு வசதியான திகதி மற்றும் 1 மணிநேர மருத்துவ நேரத்தைத் தெரிவு செய்யுங்கள்.",
      step3_title: "3. உறுதிசெய்து வருகைதாருங்கள்",
      step3_desc: "குறிப்பு எண்ணைப் பெற்று மருத்துவமனை கவுண்டரில் பணத்தைச் செலுத்துங்கள்.",
      eyebrow_why: "எங்கள் நன்மைகள்",
      title_why: "MediCare Pro-ஐ ஏன் தேர்ந்தெடுக்க வேண்டும்",
      sub_why: "முழுமையான நம்பகத்தன்மை மற்றும் வெளிப்படைத்தன்மைக்காக வடிவமைக்கப்பட்டுள்ளது.",
      b1_title: "இரட்டை முன்பதிவு இல்லை",
      b1_desc: "தரவுத்தள பூட்டுதல் மூலம் நீங்கள் தேர்ந்தெடுத்த நேரம் உடனடியாகப் பாதுகாக்கப்படும்.",
      b2_title: "நேரடி கவுண்டர் பணம்",
      b2_desc: "மறைக்கப்பட்ட கட்டணங்கள் இல்லை. மருத்துவமனை கவுண்டரில் நேரடியாகச் செலுத்துங்கள்.",
      b3_title: "தள அறிவிப்புகள்",
      b3_desc: "முன்பதிவு உறுதிப்படுத்தலைத் தளத்தில் பார்க்கலாம். மின்னஞ்சல் சேவையை அமைக்க வேண்டும்; SMS இயக்கப்படவில்லை.",
      b4_title: "SLMC பதிவுபெற்ற மருத்துவர்கள்",
      b4_desc: "அனைத்து மருத்துவ நிபுணர்களும் இலங்கை மருத்துவ சபையில் பதிவு செய்யப்பட்டவர்கள்.",
      eyebrow_testi: "நோயாளிகளின் அனுபவங்கள்",
      title_testi: "இலங்கை மக்களின் நம்பிக்கை",
      sub_testi: "MediCare Pro மூலம் பயன்பெற்ற நோயாளிகளின் உண்மையான கருத்துக்கள்.",
      eyebrow_faqs: "பொதுவான கேள்விகள்",
      title_faqs: "அடிக்கடி கேட்கப்படும் கேள்விகள்",
      sub_faqs: "முன்பதிவு, கட்டணம் மற்றும் மருத்துவமனை வருகை பற்றிய தெளிவான பதில்கள்.",
      cta_title: "மருத்துவரைச் சந்திக்கத் தயாரா?",
      cta_desc: "காத்திருப்பு நேரமின்றி உடனே மருத்துவ நிபுணரை முன்பதிவு செய்யுங்கள்.",
      cta_btn_book: "இப்போதே முன்பதிவு செய்க →",
      cta_btn_portal: "நோயாளி போர்ட்டலுக்குள் நுழைக",
      mob_search: "மருத்துவர்களைத் தேடுங்கள்",
      mob_call: "அவசர அழைப்பு 1990"
    }
  };

  function applyLanguage(lang) {
    const dict = i18nDictionary[lang] || i18nDictionary.en;
    document.querySelectorAll('[data-i18n]').forEach(el => {
      const key = el.dataset.i18n;
      if (dict[key]) {
        el.textContent = dict[key];
      }
    });

    document.querySelectorAll('.lang-btn').forEach(b => {
      b.classList.toggle('active', b.dataset.lang === lang);
    });

    localStorage.setItem('medicare_lang', lang);
  }

  document.querySelectorAll('.lang-btn').forEach(btn => {
    btn.addEventListener('click', () => applyLanguage(btn.dataset.lang));
  });

  const savedLang = localStorage.getItem('medicare_lang') || 'en';
  if (savedLang !== 'en') {
    applyLanguage(savedLang);
  }

  // =========================================================================
  // 2. DOCTOR PORTRAIT RESOLUTION & FALLBACK HANDLER
  // =========================================================================
  const localPhotos = [
    'photo-1622253692010-333f2da6031d',
    'photo-1559839734-2b71ea197ec2',
    'photo-1612349317150-e413f6a5b16d',
    'photo-1537368910025-700350fe46c7'
  ];

  const images = document.querySelectorAll('[data-portrait]');
  images.forEach((image, index) => {
    const source = image.dataset.portrait || '';
    let finalSrc = '';

    const matched = localPhotos.find(name => source.includes(name));
    if (matched) {
      finalSrc = '/images/carelink/' + matched + '.jpg';
    } else if (source.startsWith('http://') || source.startsWith('https://') || source.startsWith('/images/') || source.startsWith('data:image/')) {
      finalSrc = source;
    } else {
      const fallbackPhoto = localPhotos[index % localPhotos.length];
      finalSrc = '/images/carelink/' + fallbackPhoto + '.jpg';
    }

    image.addEventListener('load', () => { image.style.opacity = '1'; });
    image.addEventListener('error', () => {
      const fallbackPhoto = localPhotos[index % localPhotos.length];
      image.src = '/images/carelink/' + fallbackPhoto + '.jpg';
    });

    image.src = finalSrc;
  });

  // =========================================================================
  // 3. EQUAL-HEIGHT HERO SEARCH BAR & INSTANT DOCTOR FILTERING
  // =========================================================================
  const nameInput = document.getElementById('heroDoctorSearch');
  const specialtySelect = document.getElementById('heroSpecialtySelect');
  const locationSelect = document.getElementById('heroLocationSelect');
  const searchForm = document.getElementById('heroSearchForm');
  const doctorCards = document.querySelectorAll('.doc-card');
  const emptyDoctorMsg = document.getElementById('doctorsEmptyNotice');
  const clearFiltersBtn = document.getElementById('btnClearFilters');

  function applyDoctorFilter() {
    const query = (nameInput ? nameInput.value : '').trim().toLowerCase();
    const specialty = (specialtySelect ? specialtySelect.value : '').toLowerCase();
    const location = (locationSelect ? locationSelect.value : '').toLowerCase();

    let visibleCount = 0;

    doctorCards.forEach(card => {
      const cardName = (card.dataset.doctorName || '').toLowerCase();
      const cardSpecialty = (card.dataset.specialty || '').toLowerCase();
      const cardLocation = (card.dataset.location || '').toLowerCase();

      const matchesQuery = !query || cardName.includes(query) || cardSpecialty.includes(query);
      const matchesSpecialty = !specialty || cardSpecialty.includes(specialty);
      const matchesLocation = !location || cardLocation.includes(location);

      const isMatch = matchesQuery && matchesSpecialty && matchesLocation;
      card.style.display = isMatch ? 'flex' : 'none';
      if (isMatch) visibleCount++;
    });

    if (emptyDoctorMsg) {
      emptyDoctorMsg.style.display = visibleCount === 0 ? 'block' : 'none';
    }
  }

  if (nameInput) nameInput.addEventListener('input', applyDoctorFilter);
  if (specialtySelect) specialtySelect.addEventListener('change', applyDoctorFilter);
  if (locationSelect) locationSelect.addEventListener('change', applyDoctorFilter);

  if (searchForm) {
    searchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      applyDoctorFilter();
      const availableSection = document.getElementById('available-today');
      if (availableSection) {
        availableSection.scrollIntoView({ behavior: 'smooth' });
      }
    });
  }

  // =========================================================================
  // 4. HERO SEARCH TABS & POPULAR SPECIALTIES HANDLER
  // =========================================================================
  const searchTabBtns = document.querySelectorAll('.search-tab-btn');
  searchTabBtns.forEach(tab => {
    tab.addEventListener('click', () => {
      searchTabBtns.forEach(t => {
        t.classList.remove('active');
        t.setAttribute('aria-selected', 'false');
      });
      tab.classList.add('active');
      tab.setAttribute('aria-selected', 'true');

      const tabType = tab.dataset.tab;
      if (tabType === 'hospital') {
        const hospitalSec = document.getElementById('hospitals');
        if (hospitalSec) hospitalSec.scrollIntoView({ behavior: 'smooth' });
      } else if (tabType === 'booking') {
        window.location.href = '/patient/dashboard?section=booking';
      }
    });
  });

  // Popular Specialty Cards Click Handler (Exact Reference Design)
  const specCardBtns = document.querySelectorAll('.spec-card-btn[data-specialty]');
  specCardBtns.forEach(card => {
    card.addEventListener('click', () => {
      const spec = card.dataset.specialty || '';
      const isActive = card.classList.contains('active');

      specCardBtns.forEach(c => c.classList.remove('active'));

      if (!isActive) {
        card.classList.add('active');
        if (specialtySelect) specialtySelect.value = spec;
      } else {
        if (specialtySelect) specialtySelect.value = '';
      }

      applyDoctorFilter();
      const availableSection = document.getElementById('available-today');
      if (availableSection) {
        availableSection.scrollIntoView({ behavior: 'smooth' });
      }
    });
  });

  // Floating Badges Click Shortcuts
  const badgeSlmc = document.querySelector('.badge-slmc');
  if (badgeSlmc) {
    badgeSlmc.addEventListener('click', () => {
      const whySec = document.getElementById('why-choose-us');
      if (whySec) whySec.scrollIntoView({ behavior: 'smooth' });
    });
  }

  const badgeInstant = document.querySelector('.badge-instant');
  if (badgeInstant) {
    badgeInstant.addEventListener('click', () => {
      const availSec = document.getElementById('available-today');
      if (availSec) availSec.scrollIntoView({ behavior: 'smooth' });
    });
  }

  const badgeDoctors = document.querySelector('.badge-doctors');
  if (badgeDoctors) {
    badgeDoctors.addEventListener('click', () => {
      const availSec = document.getElementById('available-today');
      if (availSec) availSec.scrollIntoView({ behavior: 'smooth' });
    });
  }

  const badgeIsland = document.querySelector('.badge-island');
  if (badgeIsland) {
    badgeIsland.addEventListener('click', () => {
      const hospSec = document.getElementById('hospitals');
      if (hospSec) hospSec.scrollIntoView({ behavior: 'smooth' });
    });
  }

  if (clearFiltersBtn) {
    clearFiltersBtn.addEventListener('click', () => {
      if (nameInput) nameInput.value = '';
      if (specialtySelect) specialtySelect.value = '';
      if (locationSelect) locationSelect.value = '';
      specCardBtns.forEach(c => c.classList.remove('active'));
      applyDoctorFilter();
    });
  }

  // Specialty Cards (8 Cards in Clinical Departments section) shortcut click
  document.querySelectorAll('.specialty-card-item[data-specialty]').forEach(card => {
    card.addEventListener('click', () => {
      const spec = card.dataset.specialty || '';
      if (specialtySelect) specialtySelect.value = spec;
      applyDoctorFilter();
      const availableSection = document.getElementById('available-today');
      if (availableSection) {
        availableSection.scrollIntoView({ behavior: 'smooth' });
      }
    });
  });

  // =========================================================================
  // 5. DOCTOR PROFILE QUICK MODAL & FEE BREAKDOWN
  // =========================================================================
  const profileModal = document.getElementById('doctorProfileModal');
  const profileModalClose = document.getElementById('closeDoctorModal');
  const modalDoctorName = document.getElementById('modalDoctorName');
  const modalDoctorSpecialty = document.getElementById('modalDoctorSpecialty');
  const modalDoctorExp = document.getElementById('modalDoctorExp');
  const modalDoctorFee = document.getElementById('modalDoctorFee');
  const modalFeeDoc = document.getElementById('modalFeeDoc');
  const modalDoctorAddress = document.getElementById('modalDoctorAddress');
  const modalBookLink = document.getElementById('modalBookLink');

  document.querySelectorAll('.btn-view-profile').forEach(btn => {
    btn.addEventListener('click', () => {
      const card = btn.closest('.doc-card');
      if (!card || !profileModal) return;

      const docFeeStr = card.dataset.fee || 'LKR 2,500';
      const cleanFee = parseInt(docFeeStr.replace(/\D/g, ''), 10) || 2500;
      const baseFee = cleanFee;

      if (modalDoctorName) modalDoctorName.textContent = card.dataset.doctorName || 'Doctor';
      if (modalDoctorSpecialty) modalDoctorSpecialty.textContent = card.dataset.specialty || 'General Consultant';
      if (modalDoctorExp) modalDoctorExp.textContent = card.dataset.experience || 'Senior Consultant';
      if (modalDoctorAddress) modalDoctorAddress.textContent = card.dataset.location || 'Colombo Central Hospital';
      if (modalFeeDoc) modalFeeDoc.textContent = `LKR ${baseFee.toLocaleString()}`;
      if (modalDoctorFee) modalDoctorFee.textContent = `LKR ${cleanFee.toLocaleString()}`;
      if (modalBookLink) modalBookLink.href = '/patient/dashboard?section=booking';

      profileModal.style.display = 'flex';
    });
  });

  if (profileModalClose && profileModal) {
    profileModalClose.addEventListener('click', () => { profileModal.style.display = 'none'; });
    profileModal.addEventListener('click', (e) => {
      if (e.target === profileModal) profileModal.style.display = 'none';
    });
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape' && profileModal.style.display === 'flex') {
        profileModal.style.display = 'none';
      }
    });
  }

  // =========================================================================
  // 6. FLOATING NAVBAR — SCROLL, DROPDOWN, MOBILE PANEL
  // =========================================================================

  // 6a. Scroll → .is-scrolled class for stronger glassmorphism
  const homeHeader = document.getElementById('homeHeader');
  if (homeHeader) {
    let lastScrollY = 0;
    const onScroll = () => {
      const y = window.scrollY;
      if (y > 24 && !homeHeader.classList.contains('is-scrolled')) {
        homeHeader.classList.add('is-scrolled');
      } else if (y <= 24 && homeHeader.classList.contains('is-scrolled')) {
        homeHeader.classList.remove('is-scrolled');
      }
      lastScrollY = y;
    };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll(); // apply on load
  }

  // 6b. "More" dropdown — open on click, close on outside click/focus out
  const moreDropdown = document.querySelector('.nav-dropdown');
  const moreBtn = document.getElementById('moreDropdownBtn');
  const moreMenu = document.getElementById('moreDropdownMenu');

  if (moreDropdown && moreBtn && moreMenu) {
    moreBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      const isOpen = moreDropdown.classList.toggle('is-open');
      moreBtn.setAttribute('aria-expanded', String(isOpen));
    });

    // Close dropdown when clicking outside
    document.addEventListener('click', (e) => {
      if (!moreDropdown.contains(e.target)) {
        moreDropdown.classList.remove('is-open');
        moreBtn.setAttribute('aria-expanded', 'false');
      }
    });

    // Close dropdown when a menu item is clicked
    moreMenu.querySelectorAll('.dropdown-item').forEach(item => {
      item.addEventListener('click', () => {
        moreDropdown.classList.remove('is-open');
        moreBtn.setAttribute('aria-expanded', 'false');
      });
    });

    // Keyboard: Escape closes dropdown
    moreDropdown.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        moreDropdown.classList.remove('is-open');
        moreBtn.setAttribute('aria-expanded', 'false');
        moreBtn.focus();
      }
    });
  }

  // 6c. Mobile slide-in panel
  const mobileToggle = document.getElementById('mobileNavToggle');
  const mobilePanel = document.getElementById('mobileSlidePanel');
  const mobileBackdrop = document.getElementById('mobileMenuBackdrop');
  const mobileClose = document.getElementById('mobileNavClose');

  function openMobileMenu() {
    if (!mobilePanel || !mobileBackdrop || !mobileToggle) return;
    mobilePanel.classList.add('is-open');
    mobilePanel.setAttribute('aria-hidden', 'false');
    mobileBackdrop.classList.add('is-open');
    mobileBackdrop.setAttribute('aria-hidden', 'false');
    mobileToggle.classList.add('is-active');
    mobileToggle.setAttribute('aria-expanded', 'true');
    document.body.style.overflow = 'hidden';
  }

  function closeMobileMenu() {
    if (!mobilePanel || !mobileBackdrop || !mobileToggle) return;
    mobilePanel.classList.remove('is-open');
    mobilePanel.setAttribute('aria-hidden', 'true');
    mobileBackdrop.classList.remove('is-open');
    mobileBackdrop.setAttribute('aria-hidden', 'true');
    mobileToggle.classList.remove('is-active');
    mobileToggle.setAttribute('aria-expanded', 'false');
    document.body.style.overflow = '';
  }

  if (mobileToggle) mobileToggle.addEventListener('click', openMobileMenu);
  if (mobileClose) mobileClose.addEventListener('click', closeMobileMenu);
  if (mobileBackdrop) mobileBackdrop.addEventListener('click', closeMobileMenu);

  // Close mobile panel on link click (smooth scroll)
  if (mobilePanel) {
    mobilePanel.querySelectorAll('.mobile-nav-link').forEach(link => {
      link.addEventListener('click', () => {
        closeMobileMenu();
      });
    });
  }

  // Close mobile panel on Escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && mobilePanel && mobilePanel.classList.contains('is-open')) {
      closeMobileMenu();
    }
  });

  // 6d. Active nav link tracking on scroll (highlight current section)
  const navLinks = document.querySelectorAll('.nav-link[href^="#"]');
  const mobileNavLinkEls = document.querySelectorAll('.mobile-nav-link[href^="#"]');
  const allSectionLinks = [...navLinks, ...mobileNavLinkEls];

  if (allSectionLinks.length > 0 && 'IntersectionObserver' in window) {
    const sectionIds = [...new Set(
      allSectionLinks
        .map(a => a.getAttribute('href'))
        .filter(h => h && h !== '#')
        .map(h => h.replace('#', ''))
    )];

    const sectionObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const id = entry.target.id;
          // Update desktop nav
          navLinks.forEach(a => {
            a.classList.toggle('active', a.getAttribute('href') === '#' + id);
          });
          // Update mobile nav
          mobileNavLinkEls.forEach(a => {
            a.classList.toggle('active', a.getAttribute('href') === '#' + id);
          });
        }
      });
    }, { threshold: 0.2, rootMargin: '-80px 0px -40% 0px' });

    sectionIds.forEach(id => {
      const el = document.getElementById(id);
      if (el) sectionObserver.observe(el);
    });
  }

  // =========================================================================
  // 7. SUBTLE SCROLL-TRIGGERED REVEALS
  // =========================================================================
  if ('IntersectionObserver' in window) {
    const revealObserver = new IntersectionObserver((entries, obs) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-revealed');
          obs.unobserve(entry.target);
        }
      });
    }, { threshold: 0.08, rootMargin: '0px 0px -30px 0px' });

    document.querySelectorAll('.specialty-card-item, .doc-card, .hosp-card, .how-step-card, .benefit-card').forEach(el => {
      el.classList.add('reveal-on-scroll');
      revealObserver.observe(el);
    });
  }
});
