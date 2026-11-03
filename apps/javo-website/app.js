const nav = document.querySelector('#nav');
const menu = document.querySelector('.menu');
menu?.addEventListener('click', () => { const open = nav.classList.toggle('open'); menu.setAttribute('aria-expanded', String(open)); menu.setAttribute('aria-label', open ? 'Close navigation' : 'Open navigation'); });
document.addEventListener('keydown', e => { if(e.key==='Escape' && nav.classList.contains('open')){ nav.classList.remove('open'); menu.setAttribute('aria-expanded','false'); menu.focus(); } });
let preference;
try { preference = localStorage.getItem('javo-theme'); } catch {}
document.body.classList.toggle('light', preference === 'light');
document.querySelector('#theme')?.addEventListener('click', e => { const light = document.body.classList.toggle('light'); e.currentTarget.setAttribute('aria-label', light ? 'Use dark theme' : 'Use light theme'); try { localStorage.setItem('javo-theme', light ? 'light' : 'dark'); } catch {} });
const galleryDialog=document.querySelector('#gallery-dialog');
document.querySelectorAll('[data-gallery-filter]').forEach(button=>button.addEventListener('click',()=>{
  document.querySelectorAll('[data-gallery-filter]').forEach(b=>b.setAttribute('aria-pressed',String(b===button)));
  document.querySelectorAll('[data-gallery-category]').forEach(item=>{item.hidden=button.dataset.galleryFilter!=='all'&&item.dataset.galleryCategory!==button.dataset.galleryFilter;});
}));
document.querySelectorAll('[data-gallery-image]').forEach(button=>button.addEventListener('click',()=>{
  galleryDialog.querySelector('img').src=button.dataset.galleryImage;
  galleryDialog.querySelector('img').alt=button.dataset.galleryCaption;
  galleryDialog.querySelector('p').textContent=button.dataset.galleryCaption;
  galleryDialog.showModal();
}));
galleryDialog?.querySelector('button')?.addEventListener('click',()=>galleryDialog.close());
