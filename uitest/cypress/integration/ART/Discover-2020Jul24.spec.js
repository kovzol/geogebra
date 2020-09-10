describe('Usages of the Discover command in the arXiv paper Towards Automated Discovery of Geometrical Theorems in GeoGebra', () => {
    beforeEach(() => {
        cy.visit('index.html');
        cy.get("body.application");
    });

    afterEach(cy.setSaved);

    it("Fig 1-2-3", () => {
        cy.writeInAVInput("A=(0,0)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("C=(0,1)\n");
        cy.wait(200);
        cy.writeInAVInput("D=Midpoint(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("E=Midpoint(A,C)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(B)\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("AB ∥ DE");
           expect(text).to.include("BD = CD");
           });
    });

    it("Fig 4-5-6", () => {
        cy.writeInAVInput("A=(0,0)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("p=Polygon(A,B,6)\n");
        cy.wait(200);
        cy.writeInAVInput("G=Intersect(Line(A,D),Line(B,E))\n");
        cy.wait(200);
        cy.writeInAVInput("H=Intersect(Line(B,E),Line(C,F))\n");
        cy.wait(200);
        cy.writeInAVInput("I=Intersect(Line(A,D),Line(C,F))\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(F)\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           // FIXME: These should be checked outside the items.
           // expect(text).to.include("G=H=I");
           // expect(text).to.include("ABCDEF");
           expect(text).to.include("AB ∥ CFG ∥ DE");
           expect(text).to.include("AD = BE = CF");
           });
    });

    it("Fig 7,9", () => {
        cy.writeInAVInput("P1=(0,0)\n");
        cy.wait(200);
        cy.writeInAVInput("P2=(1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("P3=(0,1)\n");
        cy.wait(200);
        cy.writeInAVInput("P4=Intersect(Line(P3,Line(P1,P2)),Line(P1,Line(P2,P3)))\n");
        cy.wait(200);
        cy.writeInAVInput("P5=Midpoint(P1,P3)\n");
        cy.wait(200);
        cy.writeInAVInput("P6=Midpoint(P2,P4)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(P5)\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("P1P6 = P3P6"); // here P5 is expected instead of P6
           });
    });

});
