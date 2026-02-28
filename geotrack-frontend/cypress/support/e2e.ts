// Cypress E2E support file
// Add custom commands and global configuration here

// Prevent uncaught exceptions from failing tests (Angular zone errors etc.)
Cypress.on('uncaught:exception', (err) => {
  // Ignore WebSocket connection errors in tests (backend may not be running)
  if (err.message.includes('WebSocket') || err.message.includes('connection')) {
    return false;
  }
  return true;
});
