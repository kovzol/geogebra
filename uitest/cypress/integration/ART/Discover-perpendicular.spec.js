describe('Usages of the Discover command to find perpendicular lines (taken from https://matek.hu/zoltan/blog-20201019.php)', () => {
    beforeEach(() => {
        cy.visit('index.html');
        cy.get("body.application").click(10,10);
        cy.window().then((win) => {
            win.ggbApplet.setAxesVisible(false, false);
            win.ggbApplet.setGridVisible(false);
            var result = win.ggbApplet.evalCommandCAS("1+1");
            });
        cy.wait(2000);
    });

    afterEach(cy.setSaved);

    it("Thales theorem", () => {
        cy.writeInAVInput("A=(0,-0.5)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(2.5,-0.5)\n");
        cy.wait(200);
        cy.writeInAVInput("c=Circle(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("d=Line(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("D=Point(c,0.6)\n");
        cy.wait(200);
        cy.writeInAVInput("C=Intersect(c,d,2)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(D)\n");
        cy.wait(200);
        cy.writeInAVInput("\n");
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("BD ⟂ DC");
           });
    });

});
