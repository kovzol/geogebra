describe("Napoleon's theorem", () => {
    beforeEach(() => {
        cy.visit('index.html');
        cy.get("body.application");
        cy.window().then((win) => {
            var result = win.ggbApplet.evalCommandCAS("1+1");
            });
        cy.wait(2000);
    });

    afterEach(cy.setSaved);

    it("Napoleon's theorem", () => {
        cy.writeInAVInput("A=(-2,-1)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(2.5,-1)\n");
        cy.wait(200);
        cy.writeInAVInput("C=(1,2)\n");
        cy.wait(200);
        cy.writeInAVInput("t1=Polygon(A,C,3)\n");
        cy.wait(200);
        cy.writeInAVInput("t2=Polygon(C,B,3)\n");
        cy.wait(200);
        cy.writeInAVInput("t3=Polygon(B,A,3)\n");
        cy.wait(200);
        cy.writeInAVInput("p=PerpendicularBisector(A,D)\n");
        cy.wait(200);
        cy.writeInAVInput("q=PerpendicularBisector(D,C)\n");
        cy.wait(200);
        cy.writeInAVInput("r=PerpendicularBisector(C,E)\n");
        cy.wait(200);
        cy.writeInAVInput("s=PerpendicularBisector(E,B)\n");
        cy.wait(200);
        cy.writeInAVInput("t=PerpendicularBisector(B,F)\n");
        cy.wait(200);
        cy.writeInAVInput("u=PerpendicularBisector(F,A)\n");
        cy.wait(200);
        cy.writeInAVInput("G=Intersect(p,q)\n");
        cy.wait(200);
        cy.writeInAVInput("H=Intersect(r,s)\n");
        cy.wait(200);
        cy.writeInAVInput("I=Intersect(t,u)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(G)\n");
        cy.wait(200);
        cy.writeInAVInput("\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("AG = CG = DG");
           expect(text).to.include("GH = GI = HI");
           });
    });

});
