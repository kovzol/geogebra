describe('Usages of the Discover command in the RMEC paper "Discovering geometry via the Discover command in GeoGebra Discovery")', () => {
    beforeEach(() => {
        cy.visit('index.html');
        cy.get("body.application").click(10,10);
        cy.window().then((win) => {
            var result = win.ggbApplet.evalCommandCAS("1+1");
            });
        cy.wait(2000);
    });

    afterEach(cy.setSaved);

    it("Fig. 1-2-3", () => {
        cy.writeInAVInput("A=(-1,-1)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(3,1)\n");
        cy.wait(200);
        cy.writeInAVInput("sAB=PerpendicularBisector(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("AB=Line(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("H=Intersect(AB,sAB)\n");
        cy.wait(200);
        cy.writeInAVInput("P=Point(sAB)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(P)\n");
        cy.wait(200);
        cy.writeInAVInput("\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("ABH ⟂ HP");
           expect(text).to.include("AP = BP");
           });
    });

    it("Fig. 6-7-8", () => {
        cy.writeInAVInput("P=(2,3)\n");
        cy.wait(200);
        cy.writeInAVInput("Q=(1,1)\n");
        cy.wait(200);
        cy.writeInAVInput("R=(3,2)\n");
        cy.wait(200);
        cy.writeInAVInput("p=Line(Q,R)\n");
        cy.wait(200);
        cy.writeInAVInput("q=Line(P,R)\n");
        cy.wait(200);
        cy.writeInAVInput("r=Line(P,Q)\n");
        cy.wait(200);
        cy.writeInAVInput("bP=AngularBisector(Q,P,R)\n");
        cy.wait(200);
        cy.writeInAVInput("bR=AngularBisector(P,R,Q)\n");
        cy.wait(200);
        cy.writeInAVInput("X=Intersect(bP,bR)\n");
        cy.wait(200);
        cy.writeInAVInput("px=PerpendicularLine(X,p)\n");
        cy.wait(200);
        cy.writeInAVInput("qx=PerpendicularLine(X,q)\n");
        cy.wait(200);
        cy.writeInAVInput("A=Intersect(px,p)\n");
        cy.wait(200);
        cy.writeInAVInput("B=Intersect(qx,q)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(X)\n");
        cy.wait(200);
        cy.writeInAVInput("\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("XB ⟂ PRB");
           expect(text).to.include("AB ⟂ RX");
           expect(text).to.include("QRA ⟂ XA");
           expect(text).to.include("AX = BX");
           });
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           expect(text).to.include("Concyclic points: RXAB");
           });
        // cy.get(".gwt-Button button").click();
        cy.get(".DialogButtonPanel button").click();
        cy.writeInAVInput("rx=PerpendicularLine(X,r)\n");
        cy.wait(200);
        cy.writeInAVInput("C=Intersect(rx,r)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(X)\n");
        cy.wait(200);
        cy.writeInAVInput("\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("AX = BX = CX");
           });
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           expect(text).to.include("Concyclic points: RXAB, PXBC, QXAC");
           });

    });


});
