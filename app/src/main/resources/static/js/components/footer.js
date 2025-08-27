// app/src/main/resources/static/js/components/footer.js

(function () {
  function renderFooter() {
    const footer = document.getElementById("footer");
    if (!footer) return;

    footer.innerHTML = `
      <footer class="footer">
        <div class="footer-container">
          <div class="footer-logo">
            <img src="/assets/images/logo/logo.png" alt="Hospital CMS Logo" />
            <p>© ${new Date().getFullYear()}. All Rights Reserved by Hospital CMS.</p>
          </div>
          <div class="footer-links">
            <div class="footer-column">
              <h4>Company</h4>
              <a href="#">About</a>
              <a href="#">Careers</a>
              <a href="#">Press</a>
            </div>
            <div class="footer-column">
              <h4>Support</h4>
              <a href="#">Account</a>
              <a href="#">Help Center</a>
              <a href="#">Contact Us</a>
            </div>
            <div class="footer-column">
              <h4>Legals</h4>
              <a href="#">Terms & Conditions</a>
              <a href="#">Privacy Policy</a>
              <a href="#">Licensing</a>
            </div>
          </div>
        </div>
      </footer>`;
  }

  window.renderFooter = renderFooter;
  try { renderFooter(); } catch (e) { console.error(e); }
})();
