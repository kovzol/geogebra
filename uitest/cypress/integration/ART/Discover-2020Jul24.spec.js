describe('Usages of the Discover command in the arXiv paper Towards Automated Discovery of Geometrical Theorems in GeoGebra', () => {
    beforeEach(() => {
        cy.visit('index.html');
        cy.get("body.application");
    });

    afterEach(cy.setSaved);

    it("Fig 1-2-3 (Midline theorem)", () => {
        cy.writeInAVInput("A=(-4,0)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(-1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("C=(-2,3)\n");
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

    it("Fig 4-5-6 (On a regular hexagon)", () => {
        cy.writeInAVInput("A=(-3,-2)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(-1,-2)\n");
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
           expect(text).to.include("AB ∥ CFG ∥ DE");
           expect(text).to.include("AD = BE = CF");
           });
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           expect(text).to.include("Identical points: G=H=I");
           expect(text).to.include("Concyclic points: ABCDEF");
           });
    });

    it("Fig 7,9 (Diagonals of a parallelogram bisect each other)", () => {
        cy.writeInAVInput("P1=(-3,0)\n");
        cy.wait(200);
        cy.writeInAVInput("P2=(-1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("P3=(0,2)\n");
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
           expect(text).to.include("P2P6 = P4P6"); // here P5 is expected instead of P6
           });
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           expect(text).to.include("Identical points: P5=P6");
           expect(text).to.include("Collinear points: P1P3P6");
           });
    });

    it("Fig 10-11 (Euler line)", () => {
        cy.writeInAVInput("A=(-4,0)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(-1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("C=(-2,3)\n");
        cy.wait(200);
        cy.writeInAVInput("D=Midpoint(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("E=Midpoint(A,C)\n");
        cy.wait(200);
        cy.writeInAVInput("F=Midpoint(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("BC=Line(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("AC=Line(A,C)\n");
        cy.wait(200);
        cy.writeInAVInput("AB=Line(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("AM=PerpendicularLine(A,BC)\n");
        cy.wait(200);
        cy.writeInAVInput("BN=PerpendicularLine(B,AC)\n");
        cy.wait(200);
        cy.writeInAVInput("CO=PerpendicularLine(C,AB)\n");
        cy.wait(200);
        cy.writeInAVInput("M=Intersect(AM,BC)\n");
        cy.wait(200);
        cy.writeInAVInput("N=Intersect(BN,AC)\n");
        cy.wait(200);
        cy.writeInAVInput("O=Intersect(CO,AB)\n");
        cy.wait(200);
        cy.writeInAVInput("G=Intersect(AM,BN)\n");
        cy.wait(200);
        cy.writeInAVInput("H=Intersect(BN,CO)\n");
        cy.wait(200);
        cy.writeInAVInput("I=Intersect(CO,AM)\n");
        cy.wait(200);
        cy.writeInAVInput("AD=Line(A,D)\n");
        cy.wait(200);
        cy.writeInAVInput("BE=Line(B,E)\n");
        cy.wait(200);
        cy.writeInAVInput("CF=Line(C,F)\n");
        cy.wait(200);
        cy.writeInAVInput("J=Intersect(AD,BE)\n");
        cy.wait(200);
        cy.writeInAVInput("K=Intersect(BE,CF)\n");
        cy.wait(200);
        cy.writeInAVInput("L=Intersect(CF,AD)\n");
        cy.wait(200);
        cy.writeInAVInput("bBC=PerpendicularBisector(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("bAC=PerpendicularBisector(A,C)\n");
        cy.wait(200);
        cy.writeInAVInput("bAB=PerpendicularBisector(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("P=Intersect(bBC,bAC)\n");
        cy.wait(200);
        cy.writeInAVInput("Q=Intersect(bAC,bAB)\n");
        cy.wait(200);
        cy.writeInAVInput("R=Intersect(bAB,bBC)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(P)\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("AMG ∥ DQ");
           expect(text).to.include("BNG ∥ EQ");
           expect(text).to.include("COG ∥ FQ");
           expect(text).to.include("AQ = BQ = CQ");
           });
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           expect(text).to.include("Identical points: G=H=I, J=K=L, P=Q=R");
           expect(text).to.include("Collinear points: GJQ");
           expect(text).to.include("Concyclic points: BDFQ, AEFQ, CDEQ");
           });
    });

    it("Fig 12-13 (Nine-point circle)", () => {
        cy.writeInAVInput("A=(-4,0)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(-1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("C=(-2,3)\n");
        cy.wait(200);
        cy.writeInAVInput("D=Midpoint(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("E=Midpoint(A,C)\n");
        cy.wait(200);
        cy.writeInAVInput("F=Midpoint(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("BC=Line(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("AC=Line(A,C)\n");
        cy.wait(200);
        cy.writeInAVInput("AB=Line(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("AM=PerpendicularLine(A,BC)\n");
        cy.wait(200);
        cy.writeInAVInput("BN=PerpendicularLine(B,AC)\n");
        cy.wait(200);
        cy.writeInAVInput("CO=PerpendicularLine(C,AB)\n");
        cy.wait(200);
        cy.writeInAVInput("M=Intersect(AM,BC)\n");
        cy.wait(200);
        cy.writeInAVInput("N=Intersect(BN,AC)\n");
        cy.wait(200);
        cy.writeInAVInput("O=Intersect(CO,AB)\n");
        cy.wait(200);
        cy.writeInAVInput("G=Intersect(AM,BN)\n");
        cy.wait(200);
        cy.writeInAVInput("H=Intersect(BN,CO)\n");
        cy.wait(200);
        cy.writeInAVInput("I=Intersect(CO,AM)\n");
        cy.wait(200);
        cy.writeInAVInput("AD=Line(A,D)\n");
        cy.wait(200);
        cy.writeInAVInput("BE=Line(B,E)\n");
        cy.wait(200);
        cy.writeInAVInput("CF=Line(C,F)\n");
        cy.wait(200);
        cy.writeInAVInput("J=Midpoint(A,G)\n");
        cy.wait(200);
        cy.writeInAVInput("K=Midpoint(B,G)\n");
        cy.wait(200);
        cy.writeInAVInput("L=Midpoint(C,G)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(D)\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("ACEN ∥ DF ∥ JL");
           expect(text).to.include("ABFO ∥ DE ∥ JK");
           expect(text).to.include("COGL ∥ DK ∥ EJ");
           expect(text).to.include("BNGK ∥ DL ∥ FJ");
           expect(text).to.include("BCDM ∥ EF ∥ KL");
           expect(text).to.include("AF = BF = DE = FM = FN = JK");
           expect(text).to.include("DJ = EK = FL");
           });
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           expect(text).to.include("Identical points: G=H=I");
           expect(text).to.include("Concyclic points: DEFMNOJKL"); // fix order
           });
    });

    // This times out on web, but works well on desktop.
    it("Fig 14-15 (IMO 2010 problem)", () => {
        cy.writeInAVInput("A=(-4,0)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(-1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("C=(-3,3)\n");
        cy.wait(200);
        cy.writeInAVInput("BC=Line(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("AC=Line(A,C)\n");
        cy.wait(200);
        cy.writeInAVInput("AB=Line(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("AD=PerpendicularLine(A,BC)\n");
        cy.wait(200);
        cy.writeInAVInput("BE=PerpendicularLine(B,AC)\n");
        cy.wait(200);
        cy.writeInAVInput("CF=PerpendicularLine(C,AB)\n");
        cy.wait(200);
        cy.writeInAVInput("D=Intersect(AD,BC)\n");
        cy.wait(200);
        cy.writeInAVInput("E=Intersect(BE,AC)\n");
        cy.wait(200);
        cy.writeInAVInput("F=Intersect(CF,AB)\n");
        cy.wait(200);
        cy.writeInAVInput("cABC=Circle(A,B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("P=Intersect(cABC,Line(E,F),2)\n");
        cy.wait(200);
        cy.writeInAVInput("Q=Intersect(Line(B,P),Line(D,F))\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(Q)\n");
        cy.wait(200);
        cy.get(".RelationTool").should(($div) => {
           const text = $div.text();
           expect(text).to.include("DP ∥ EQ");
           expect(text).to.include("AP = AQ");
           });
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           // expect(text).to.include("Concyclic points: AFPQ, CDPQ");
           expect(text).to.include("AFPQ");
           expect(text).to.include("CDPQ");
           });
    });

    // This times out on web, but works on desktop with increased prover timeout setting.
    it("Fig 16-17 (Pappus)", () => {
        cy.writeInAVInput("A=(-4,3)\n");
        cy.wait(200);
        cy.writeInAVInput("B=(-1,3)\n");
        cy.wait(200);
        cy.writeInAVInput("AB=Line(A,B)\n");
        cy.wait(200);
        cy.writeInAVInput("E=Point(AB)\n");
        cy.wait(200);
        cy.writeInAVInput("C=(-4,0)\n");
        cy.wait(200);
        cy.writeInAVInput("D=(-1,0)\n");
        cy.wait(200);
        cy.writeInAVInput("CD=Line(C,D)\n");
        cy.wait(200);
        cy.writeInAVInput("F=Point(CD)\n");
        cy.wait(200);
        cy.writeInAVInput("AD=Line(A,D)\n");
        cy.wait(200);
        cy.writeInAVInput("AF=Line(A,F)\n");
        cy.wait(200);
        cy.writeInAVInput("BC=Line(B,C)\n");
        cy.wait(200);
        cy.writeInAVInput("BF=Line(B,F)\n");
        cy.wait(200);
        cy.writeInAVInput("CE=Line(C,E)\n");
        cy.wait(200);
        cy.writeInAVInput("DE=Line(D,E)\n");
        cy.wait(200);
        cy.writeInAVInput("G=Intersect(AD,BC)\n");
        cy.wait(200);
        cy.writeInAVInput("H=Intersect(AF,CE)\n");
        cy.wait(200);
        cy.writeInAVInput("I=Intersect(BF,DE)\n");
        cy.wait(200);
        cy.writeInAVInput("Discover(G)\n");
        cy.wait(200);
        cy.get(".gwt-HTML").should(($div) => {
           const text = $div.text();
           expect(text).to.include("Collinear points: GHI");
           });
    });

});