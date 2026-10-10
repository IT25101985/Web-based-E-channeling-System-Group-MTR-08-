document.addEventListener('DOMContentLoaded', () => {
 const patient=document.getElementById('receptionPatient');
 patient?.addEventListener('change',()=>{
  const option=patient.selectedOptions[0];
  document.getElementById('receptionEmail').value=option?.dataset.email||'';
  document.getElementById('receptionPhone').value=option?.dataset.phone||'';
 });
});
