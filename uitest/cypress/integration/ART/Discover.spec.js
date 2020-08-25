describe('Discovery in regular polygons', () => {
    beforeEach(() => {
        cy.visit('index.html');
        cy.get("body.application");
        cy.writeInAVInput("A=(0,0)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(1,0)\n");
        cy.wait(200);
    });

    afterEach(cy.setSaved);

    it("The sides of a regular triangle are of equal length", () => {
        cy.writeInAVInput("p=Polygon(A,B,3)\n");
        cy.wait(200);
        cy.window().then((win) => {
            win.ggbApplet.asyncEvalCommand("Discover(B)");
            cy.get(".RelationTool").contains("AB = AC = BC");
        }
    });

    it("The sides are of a square are of equal length", () => {
        cy.writeInAVInput("p=Polygon(A,B,4)\n");
        cy.wait(200);
        cy.window().then((win) => {
            win.ggbApplet.asyncEvalCommand("Discover(B)");
            cy.get(".RelationTool").contains("AB = AD = BC = CD");
            cy.get(".RelationTool").contains("AC = BD");
        }
    });

});
