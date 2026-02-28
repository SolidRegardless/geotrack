/**
 * E2E tests for core navigation and page rendering.
 * Verifies all routes load correctly and the navbar functions.
 */
describe('Navigation', () => {
  it('should redirect root to /map', () => {
    cy.visit('/');
    cy.url().should('include', '/map');
  });

  it('should navigate to all main routes via navbar', () => {
    cy.visit('/');

    // Navigate to Assets
    cy.get('nav').contains('Assets').click();
    cy.url().should('include', '/assets');
    cy.get('h2, h1').should('contain.text', 'Asset');

    // Navigate to Alerts
    cy.get('nav').contains('Alerts').click();
    cy.url().should('include', '/alerts');
    cy.get('h2, h1').should('contain.text', 'Alert');

    // Navigate to Dashboard
    cy.get('nav').contains('Dashboard').click();
    cy.url().should('include', '/dashboard');

    // Navigate back to Map
    cy.get('nav').contains('Map').click();
    cy.url().should('include', '/map');
  });

  it('should display the navbar on all pages', () => {
    const routes = ['/map', '/assets', '/alerts', '/dashboard'];

    routes.forEach(route => {
      cy.visit(route);
      cy.get('nav').should('be.visible');
    });
  });
});
