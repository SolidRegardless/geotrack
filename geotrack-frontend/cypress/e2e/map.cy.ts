/**
 * E2E tests for the tracking map view.
 * Verifies Leaflet map initialisation and basic interaction.
 */
describe('Tracking Map', () => {
  beforeEach(() => {
    cy.visit('/map');
  });

  it('should render the Leaflet map container', () => {
    // Leaflet creates a container with class 'leaflet-container'
    cy.get('.leaflet-container', { timeout: 15000 }).should('be.visible');
  });

  it('should display map tiles', () => {
    cy.get('.leaflet-tile-pane', { timeout: 15000 }).should('exist');
    // Tiles should be loading or loaded
    cy.get('.leaflet-tile').should('have.length.greaterThan', 0);
  });

  it('should have zoom controls', () => {
    cy.get('.leaflet-control-zoom').should('be.visible');
    cy.get('.leaflet-control-zoom-in').should('be.visible');
    cy.get('.leaflet-control-zoom-out').should('be.visible');
  });

  it('should respond to zoom interactions', () => {
    cy.get('.leaflet-control-zoom-in').click();
    // Map should still be visible after zoom
    cy.get('.leaflet-container').should('be.visible');
  });
});
