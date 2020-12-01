describe('Discovery in regular polygons', () => {
    beforeEach(() => {
        cy.visit('index.html');
        cy.get("body.application").click(10,10);
        cy.window().then((win) => {
            var result = win.ggbApplet.evalCommandCAS("1+1");
            });
        cy.wait(2000);

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

    it("The sides and the diagonals of a square are of equal length", () => {
        cy.writeInAVInput("p=Polygon(A,B,4)\n");
        cy.wait(200);
        cy.window().then((win) => {
            win.ggbApplet.asyncEvalCommand("Discover(B)");
            cy.get(".RelationTool").contains("AB = AD = BC = CD");
            cy.get(".RelationTool").contains("AC = BD");
        }
    });

    it("The sides and the diagonals of a square are of equal length (by typing)", () => {
        cy.writeInAVInput("p=Polygon(A,B,4)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(B)\n");
        cy.get(".RelationTool").contains("AB = AD = BC = CD");
        cy.get(".RelationTool").contains("AC = BD");
    });

    it("The diagonals of a regular pentagon are of equal length (by typing)", () => {
        cy.writeInAVInput("p=Polygon(A,B,5)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(B)\n");
        cy.get(".RelationTool").contains("AC = AD = BD = BE = CE");
    });

    it("The diagonals of a regular pentagon are parallel to a given side (by typing)", () => {
        cy.writeInAVInput("p=Polygon(A,B,5)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(B)\n");
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("AD ∥ BC");
           expect(text).to.include("AE ∥ BD");
           expect(text).to.include("BE ∥ CD");
           expect(text).to.include("AB ∥ CE");
           });
    });

});
