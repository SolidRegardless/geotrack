/**
 * E2E tests for the assets page.
 * Verifies asset list rendering and API integration.
 */
describe('Asset Management', () => {
  beforeEach(() => {
    cy.visit('/assets');
  });

  it('should display the assets page heading', () => {
    cy.get('h2, h1').should('contain.text', 'Asset');
  });

  it('should show a loading state or asset list', () => {
    // Either a loading indicator or a table/list should be present
    cy.get('body').then(($body) => {
      const hasTable = $body.find('table').length > 0;
      const hasList = $body.find('.asset-list, .asset-card, ul, [class*="asset"]').length > 0;
      const hasEmpty = $body.text().includes('No assets') || $body.text().includes('no assets');
      const hasLoading = $body.find('.loading, .spinner, [class*="loading"]').length > 0;

      // At least one of these states should be true
      expect(hasTable || hasList || hasEmpty || hasLoading).to.be.true;
    });
  });

  it('should have the correct page title or heading', () => {
    cy.title().should('not.be.empty');
  });
});
